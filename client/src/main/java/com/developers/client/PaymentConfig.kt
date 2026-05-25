package com.developers.client

object PaymentConfig {
    // Reemplaza estas cadenas con tus claves reales de Stripe.
    // Puedes encontrarlas en: https://dashboard.stripe.com/test/apikeys
    
    const val PUBLISHABLE_KEY = "ADD_YOUR_PUBLISHABLE_KEY_HERE"
    
    // NOTA: El Secret Key NO debe incluirse en la aplicación cliente por seguridad.
    // Se incluye aquí solo con fines ilustrativos para este ejercicio.
    const val SECRET_KEY = "ADD_YOUR_SECRET_KEY_HERE"
}
