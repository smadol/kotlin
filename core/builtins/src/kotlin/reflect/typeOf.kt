/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

@Suppress("unused")
inline fun <reified T> typeOf(): KType =
    throw UnsupportedOperationException("This function is implemented as an intrinsic on all supported platforms.")
