/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirProviderImpl(val session: FirSession) : FirProvider {

    override fun getFirClassifierBySymbol(symbol: ConeSymbol): FirNamedDeclaration? {
        return when (symbol) {
            is FirBasedSymbol<*> -> symbol.fir as? FirNamedDeclaration
            is ConeClassLikeSymbol -> getFirClassifierByFqName(symbol.classId)
            else -> error("!")
        }
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
        return (getFirClassifierByFqName(classId) as? FirSymbolOwner<*>)?.symbol as? ConeClassLikeSymbol
    }

    override fun getCallableSymbols(callableId: CallableId): List<ConeCallableSymbol> {
        return (callableMap[callableId] ?: emptyList())
            .filterIsInstance<FirSymbolOwner<*>>()
            .mapNotNull { it.symbol as? ConeCallableSymbol }
    }

    override fun getTypeParameterSymbol(owner: ConeSymbol, name: Name): ConeTypeParameterSymbol? {
        return typeParameterMap[owner to name]?.symbol
    }

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        return classifierContainerFileMap[fqName] ?: error("Couldn't find container for $fqName")
    }

    fun recordFile(file: FirFile) {
        val packageName = file.packageFqName
        fileMap.merge(packageName, listOf(file)) { a, b -> a + b }

        file.acceptChildren(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {}

            var containerFqName: FqName = FqName.ROOT

            var containerSymbol: ConeSymbol? = null

            private fun withContainerSymbol(symbol: ConeSymbol, f: () -> Unit) {
                val previousContainer = containerSymbol
                containerSymbol = symbol
                f()
                containerSymbol = previousContainer
            }

            override fun visitRegularClass(regularClass: FirRegularClass) {
                val fqName = containerFqName.child(regularClass.name)
                val classId = ClassId(packageName, fqName, false)
                classifierMap[classId] = regularClass
                classifierContainerFileMap[classId] = file

                containerFqName = fqName
                withContainerSymbol(regularClass.symbol) {
                    regularClass.acceptChildren(this)
                }
                containerFqName = fqName.parent()
            }

            override fun visitTypeAlias(typeAlias: FirTypeAlias) {
                val fqName = containerFqName.child(typeAlias.name)
                val classId = ClassId(packageName, fqName, false)
                classifierMap[classId] = typeAlias
                classifierContainerFileMap[classId] = file
                withContainerSymbol(typeAlias.symbol) {
                    typeAlias.acceptChildren(this)
                }
            }

            override fun visitCallableMember(callableMember: FirCallableMember) {
                val callableId = when (containerFqName) {
                    FqName.ROOT -> CallableId(packageName, callableMember.name)
                    else -> CallableId(packageName, containerFqName, callableMember.name)
                }
                callableMap.merge(callableId, listOf(callableMember)) { a, b -> a + b }
                withContainerSymbol(callableMember.symbol) {
                    callableMember.acceptChildren(this)
                }
            }

            override fun visitNamedFunction(namedFunction: FirNamedFunction) {
                visitCallableMember(namedFunction)
            }

            override fun visitProperty(property: FirProperty) {
                visitCallableMember(property)
            }

            override fun visitTypeParameter(typeParameter: FirTypeParameter) {
                containerSymbol?.let { typeParameterMap[it to typeParameter.name] = typeParameter }
            }
        })
    }

    private val fileMap = mutableMapOf<FqName, List<FirFile>>()
    private val classifierMap = mutableMapOf<ClassId, FirMemberDeclaration>()
    private val classifierContainerFileMap = mutableMapOf<ClassId, FirFile>()
    private val callableMap = mutableMapOf<CallableId, List<FirNamedDeclaration>>()
    private val typeParameterMap = mutableMapOf<Pair<ConeSymbol, Name>, FirTypeParameter>()

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> {
        return fileMap[fqName].orEmpty()
    }

    override fun getFirClassifierByFqName(fqName: ClassId): FirMemberDeclaration? {
        return classifierMap[fqName]
    }

}