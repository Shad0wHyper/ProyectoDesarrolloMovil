# Valoraciones de compras entregadas

La app guarda un voto por cuenta y producto en
`productos/{productId}/valoraciones/{uid}`. El voto contiene `estrellas`,
`pedidoId` y `partida` (el índice original en `items`). Los pedidos históricos
sin `productoId` en la partida no pueden acreditar qué producto se compró.
El catálogo excluye votos anteriores que no tengan el vínculo con el pedido.

El checkout actual guarda `productoId` en cada partida. Para habilitar el
flujo en adelante, integra las reglas de abajo y prueba con un pedido nuevo.
Los pedidos antiguos incompletos pueden quedarse así. La migración que se
explica más abajo es opcional.

Este repositorio no contiene las reglas desplegadas. Integra este bloque en
`match /databases/{database}/documents`, dentro de las reglas existentes.

```text
match /productos/{productId}/valoraciones/{uid} {
  function registeredUser() {
    return request.auth != null
      && request.auth.token.firebase.sign_in_provider != 'anonymous';
  }

  function ownCustomerRating() {
    return registeredUser()
      && request.auth.uid == uid
      && get(/databases/$(database)/documents/usuarios/$(uid)).data.rol == 'cliente';
  }

  function purchasedAndDelivered() {
    let vote = request.resource.data;
    let order = get(/databases/$(database)/documents/usuarios/$(uid)/pedidos/$(vote.pedidoId)).data;
    return order.userId == uid
      && order.estado == 'ENTREGADO'
      && order.items is list
      && vote.partida < order.items.size()
      && order.items[vote.partida].productoId == productId;
  }

  allow get: if registeredUser() && request.auth.uid == uid;
  allow list: if registeredUser();

  allow create, update: if ownCustomerRating()
    && request.resource.data.keys().hasOnly(['estrellas', 'pedidoId', 'partida'])
    && request.resource.data.estrellas is int
    && request.resource.data.estrellas >= 1
    && request.resource.data.estrellas <= 5
    && request.resource.data.pedidoId is string
    && request.resource.data.pedidoId.matches('^[A-Za-z0-9_-]+$')
    && request.resource.data.partida is int
    && request.resource.data.partida >= 0
    && exists(/databases/$(database)/documents/productos/$(productId))
    && purchasedAndDelivered();
}
```

El permiso de listado para agregaciones también permite leer los documentos
individuales, cuyos identificadores son UID. Si deben mantenerse privados,
publica solo agregados calculados en el backend. Comprueba que ninguna regla
más amplia permita alterar votos ajenos y que un cliente no pueda cambiar el
estado ni las partidas de un pedido entregado, ni su propio rol.

## Habilitar valoraciones en pedidos históricos

El script [backfill-order-product-ids.cjs](backfill-order-product-ids.cjs) prepara
una lista de vínculos para partidas de pedidos entregados que carezcan de
`productoId` y cuyo nombre coincida con un único producto actual. Comprueba
que `userId` corresponda con el propietario en la ruta. Los casos ambiguos
o sin correspondencia requieren comprobación en el registro original.

Usa Node.js, el paquete `firebase-admin` y credenciales de aplicación de un
operador autorizado en el proyecto correcto. Ejecuta primero en un entorno de
pruebas. El paso `plan` solo lee Firestore y genera JSON por la salida estándar.
Conserva el JSON fuera del repositorio, revisa los vínculos contra los tickets
originales y elimina las entradas dudosas antes de ejecutar `apply`.

```powershell
node docs/backfill-order-product-ids.cjs plan PROJECT_ID | Set-Content -Encoding utf8NoBOM PLAN.json
node docs/backfill-order-product-ids.cjs apply PROJECT_ID PLAN.json
```

Si Firestore solicita un índice para la consulta de pedidos entregados, usa el
enlace del error para crear el índice de ámbito de grupo de colecciones.
La escritura vuelve a validar el estado y cada vínculo en una transacción.
Solo agrega el identificador del producto a partidas aprobadas. Cuando el
cliente vuelva a abrir su pedido entregado podrá registrar su propia valoración.
Las consultas de estrellas en Cliente y Empleado usan el permiso de listado del
bloque anterior.

## Más pedidos

El checkout incrementa `productos/{productId}.pedidosRegistrados` junto al
descuento de existencias, una vez por producto y pedido, incluso para pedidos
registrados pendientes de pago. Esto mide pedidos registrados y no ventas
pagadas ni unidades. No hay relleno histórico. Revisa que la regla existente
de actualización de catálogo durante checkout admita el nuevo campo. Si se
requiere un ranking resistente a manipulación, el contador debe calcularse en
un backend de confianza a partir de las altas de pedidos.

Prueba con dos cuentas: entrega un pedido en el flujo de empleado, vota desde
el historial del cliente y verifica que el catálogo refleje el cambio. Edita
el voto y comprueba que el recuento no aumente. Comprueba que las reglas rechacen
un pedido ajeno, sin entregar o que no contenga el producto.
