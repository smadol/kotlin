/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractProviderBasedScope
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConePropertySymbol
import org.jetbrains.kotlin.name.Name

class FirJavaEnhancementScope(
    session: FirSession,
    private val unsubstituted: FirScope
) : FirAbstractProviderBasedScope(session, lookupInFir = true) {

    private val enhancements = mutableMapOf<ConeCallableSymbol, ConeCallableSymbol>()

    private fun createFunctionEnhancement(original: ConeFunctionSymbol, name: Name): ConeFunctionSymbol {
        // TODO
        return original
    }

    override fun processPropertiesByName(name: Name, processor: (ConePropertySymbol) -> ProcessorAction): ProcessorAction {
        return unsubstituted.processPropertiesByName(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        unsubstituted.processFunctionsByName(name) { original ->
            val function = enhancements.getOrPut(original) { createFunctionEnhancement(original, name) }
            processor(function as ConeFunctionSymbol)
        }

        return unsubstituted.processFunctionsByName(name, processor)
    }
}