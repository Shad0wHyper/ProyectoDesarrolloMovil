const { readFileSync } = require('node:fs');
const { applicationDefault, initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');

const [mode, projectId, planPath] = process.argv.slice(2);
const customerOrderPath = /^usuarios\/([^/]+)\/pedidos\/([^/]+)$/;
const productName = value => typeof value === 'string' && value.trim() ? value : '';

async function plan(db) {
    const catalog = await db.collection('productos').get();
    const idsByName = new Map();
    for (const product of catalog.docs) {
        const name = productName(product.get('nombre'));
        if (!name) continue;
        const ids = idsByName.get(name) ?? [];
        ids.push(product.id);
        idsByName.set(name, ids);
    }

    const orders = await db.collectionGroup('pedidos').where('estado', '==', 'ENTREGADO').get();
    const links = [];
    let unresolved = 0;
    for (const order of orders.docs) {
        const path = customerOrderPath.exec(order.ref.path);
        if (!path || order.get('userId') !== path[1]) continue;
        const items = order.get('items');
        if (!Array.isArray(items)) continue;
        items.forEach((item, index) => {
            if (!item || item.productoId) return;
            const name = productName(item.nombre);
            const ids = idsByName.get(name);
            if (ids?.length === 1) {
                links.push({ path: order.ref.path, index, name, productId: ids[0] });
            } else {
                unresolved++;
            }
        });
    }
    process.stdout.write(JSON.stringify({ projectId, unresolved, links }, null, 2) + '\n');
}

async function apply(db) {
    const approved = JSON.parse(readFileSync(planPath, 'utf8'));
    if (approved.projectId !== projectId || !Array.isArray(approved.links)) {
        throw new Error('Invalid plan or project mismatch.');
    }
    const byOrder = new Map();
    for (const link of approved.links) {
        if (typeof link.path !== 'string' || !customerOrderPath.test(link.path) ||
            !Number.isSafeInteger(link.index) || link.index < 0 || !productName(link.name) ||
            typeof link.productId !== 'string' || !link.productId || link.productId.includes('/')) {
            throw new Error('Invalid plan entry.');
        }
        const links = byOrder.get(link.path) ?? new Map();
        if (links.has(link.index)) throw new Error('Duplicate plan entry.');
        links.set(link.index, link);
        byOrder.set(link.path, links);
    }

    let updated = 0;
    for (const [path, links] of byOrder) {
        updated += await db.runTransaction(async transaction => {
            const ref = db.doc(path);
            const order = await transaction.get(ref);
            const items = order.get('items');
            if (order.get('estado') !== 'ENTREGADO' ||
                order.get('userId') !== customerOrderPath.exec(path)[1] ||
                !Array.isArray(items)) return 0;
            let changes = 0;
            for (const [index, link] of links) {
                const item = items[index];
                if (!item || item.productoId || productName(item.nombre) !== link.name) continue;
                const matches = await transaction.get(
                    db.collection('productos').where('nombre', '==', link.name).limit(2)
                );
                if (matches.size !== 1 || matches.docs[0].id !== link.productId) continue;
                items[index] = { ...item, productoId: link.productId };
                changes++;
            }
            if (changes > 0) transaction.update(ref, { items });
            return changes;
        });
    }
    console.log(`Updated order lines: ${updated}`);
}

async function main() {
    if (!projectId || !['plan', 'apply'].includes(mode) || (mode === 'apply' && !planPath)) {
        throw new Error('Usage: node backfill-order-product-ids.cjs plan PROJECT_ID | apply PROJECT_ID PLAN.json');
    }
    initializeApp({ projectId, credential: applicationDefault() });
    const db = getFirestore();
    await (mode === 'plan' ? plan(db) : apply(db));
}

main().catch(error => {
    console.error(error.message);
    process.exitCode = 1;
});
