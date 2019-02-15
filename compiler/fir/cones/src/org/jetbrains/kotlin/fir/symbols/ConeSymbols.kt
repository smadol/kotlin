/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

interface ConeSymbol : TypeConstructorMarker

interface ConeTypeParameterSymbol : ConeSymbol, TypeParameterMarker {
    val name: Name
}

interface ConeClassLikeSymbol : ConeSymbol, TypeConstructorMarker {
    val classId: ClassId
}

interface ConeTypeAliasSymbol : ConeClassLikeSymbol

interface ConeClassSymbol : ConeClassLikeSymbol
