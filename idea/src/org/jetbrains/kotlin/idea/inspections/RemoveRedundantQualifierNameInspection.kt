/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.scopes.utils.findFirstClassifierWithDeprecationStatus

class RemoveRedundantQualifierNameInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                if (expression.parent is KtDotQualifiedExpression? || expression.isInImportDirective()) return

                val context = expression.analyze()
                val descriptor = expression.getQualifiedElementSelector()?.mainReference
                    ?.resolveToDescriptors(context)?.firstOrNull() ?: return

                val applicableExpression = expression.firstApplicableExpression(validator = {
                    applicableExpression(expression, context, descriptor)
                }) {
                    firstChild as? KtDotQualifiedExpression
                } ?: return

                reportProblem(holder, applicableExpression)
            }

            override fun visitUserType(type: KtUserType) {
                if (type.parent is KtUserType) return

                val applicableExpression = type.firstApplicableExpression(KtUserType::applicableExpression) {
                    firstChild as? KtUserType
                } ?: return

                reportProblem(holder, applicableExpression)
            }
        }
}

private tailrec fun <T : KtElement> T.firstApplicableExpression(validator: T.() -> T?, generator: T.() -> T?): T? =
    validator() ?: generator()?.firstApplicableExpression(validator, generator)

private fun KtDotQualifiedExpression.applicableExpression(
    originalExpression: KtExpression,
    oldContext: BindingContext,
    descriptor: DeclarationDescriptor
): KtDotQualifiedExpression? {
    if (receiverExpression is KtThisExpression || !ShortenReferences.canBePossibleToDropReceiver(this, oldContext)) return null
    val expressionText = originalExpression.text.substring(lastChild.startOffset - originalExpression.startOffset)
    val newExpression = KtPsiFactory(originalExpression).createExpression(expressionText)
    val newContext = newExpression.analyzeAsReplacement(this, oldContext)
    val newDescriptor = newExpression.getQualifiedElementSelector()?.mainReference
        ?.resolveToDescriptors(newContext)?.firstOrNull() ?: return null

    return takeIf {
        newDescriptor.let {
            if (it is ImportedFromObjectCallableDescriptor<*>) it.callableFromObject else it
        } == descriptor
    }
}

private fun KtUserType.applicableExpression(): KtUserType? {
    if (firstChild !is KtUserType) return null
    val referenceExpression = referenceExpression as? KtNameReferenceExpression ?: return null
    val originalDescriptor = referenceExpression.resolveMainReferenceToDescriptors().firstOrNull() ?: return null

    val shortName = originalDescriptor.importableFqName?.shortName() ?: return null
    val scope = referenceExpression.getResolutionScope()
    val descriptor = scope.findFirstClassifierWithDeprecationStatus(shortName, NoLookupLocation.FROM_IDE)?.descriptor ?: return null
    return if (descriptor == originalDescriptor) this else null
}

private fun reportProblem(holder: ProblemsHolder, element: KtElement) {
    val firstChild = element.firstChild
    holder.registerProblem(
        element,
        "Redundant qualifier name",
        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
        TextRange.from(firstChild.startOffsetInParent, firstChild.textLength + 1),
        RemoveRedundantQualifierNameQuickFix()
    )
}

class RemoveRedundantQualifierNameQuickFix : LocalQuickFix {
    override fun getName() = "Remove redundant qualifier name"
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement.containingFile as KtFile
        val range = when (val element = descriptor.psiElement) {
            is KtUserType -> IntRange(descriptor.psiElement.startOffset, element.referenceExpression?.endOffset ?: return)
            is KtDotQualifiedExpression -> IntRange(
                descriptor.psiElement.startOffset,
                element.selectorExpression?.referenceExpression()?.endOffset ?: return
            )
            else -> IntRange.EMPTY
        }

        val substring = file.text.substring(range.start, range.endInclusive)
        Regex.fromLiteral(substring).findAll(file.text, file.importList?.endOffset ?: 0).toList().reversed().forEach {
            ShortenReferences.DEFAULT.process(file, it.range.start, it.range.endInclusive + 1)
        }
    }
}