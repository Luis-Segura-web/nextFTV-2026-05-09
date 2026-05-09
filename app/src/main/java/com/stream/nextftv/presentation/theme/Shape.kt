package com.stream.nextftv.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Definimos las formas estándar.
// Nota: Aunque los valores base están en dimens.xml, en Compose Shapes se suelen definir 
// aquí estáticamente para permitir optimizaciones del compilador.
// Si quisiéramos leer de XML estrictamente, tendríamos que inyectarlos, pero 
// para Shapes esto es el estándar "Clean Compose".
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
