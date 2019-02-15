/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.load.java.structure.JavaType

internal fun FirResolvedTypeRef.enhanceReturnType(javaType: JavaType): FirResolvedTypeRef {
    return enhanceType(javaType)
}

internal fun FirResolvedTypeRef.enhanceParameterType(javaType: JavaType): FirResolvedTypeRef {
    return enhanceType(javaType)
}

private fun FirResolvedTypeRef.enhanceType(javaType: JavaType): FirResolvedTypeRef {
    val type = type
    if (type is ConeKotlinErrorType || type is ConeClassErrorType) return this
    return when (type) {
        is ConeFlexibleType -> {
            val lowerBound = type.lowerBound
            val enhancedLowerBound = enhanceInflexibleType(lowerBound, javaType)
            val upperBound = type.upperBound
            val enhancedUpperBound = enhanceInflexibleType(upperBound, javaType)
            if (enhancedLowerBound === lowerBound && enhancedUpperBound === upperBound) {
                this
            } else {
                FirResolvedTypeRefImpl(
                    session, psi,
                    ConeFlexibleType(enhancedLowerBound, enhancedUpperBound),
                    isMarkedNullable, annotations
                )
            }
        }
        else -> {
            val enhanced = enhanceInflexibleType(type, javaType)
            if (enhanced === type) {
                this
            } else {
                FirResolvedTypeRefImpl(session, psi, enhanced, isMarkedNullable, annotations)
            }
        }
    }
}

private fun FirResolvedTypeRef.enhanceInflexibleType(type: ConeKotlinType, javaType: JavaType): ConeKotlinType {
    return type // TODO
}

