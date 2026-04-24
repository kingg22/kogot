# Godot Kotlin Native C Interop Signatures

```
package io.github.kingg22.godot.internal.ffi

import kotlinx.cinterop.*
import platform.posix.*

typealias char32_tVar = UIntVarOf<char32_t>

typealias char32_t = uint32_t

typealias char16_tVar = UShortVarOf<char16_t>

typealias char16_t = uint16_t

typealias GDExtensionVariantPtrVar = CPointerVarOf<GDExtensionVariantPtr>

typealias GDExtensionVariantPtr = COpaquePointer

typealias GDExtensionConstVariantPtrVar = CPointerVarOf<GDExtensionConstVariantPtr>

typealias GDExtensionConstVariantPtr = COpaquePointer

typealias GDExtensionUninitializedVariantPtrVar = CPointerVarOf<GDExtensionUninitializedVariantPtr>

typealias GDExtensionUninitializedVariantPtr = COpaquePointer

typealias GDExtensionStringNamePtrVar = CPointerVarOf<GDExtensionStringNamePtr>

typealias GDExtensionStringNamePtr = COpaquePointer

typealias GDExtensionConstStringNamePtrVar = CPointerVarOf<GDExtensionConstStringNamePtr>

typealias GDExtensionConstStringNamePtr = COpaquePointer

typealias GDExtensionUninitializedStringNamePtrVar = CPointerVarOf<GDExtensionUninitializedStringNamePtr>

typealias GDExtensionUninitializedStringNamePtr = COpaquePointer

typealias GDExtensionStringPtrVar = CPointerVarOf<GDExtensionStringPtr>

typealias GDExtensionStringPtr = COpaquePointer

typealias GDExtensionConstStringPtrVar = CPointerVarOf<GDExtensionConstStringPtr>

typealias GDExtensionConstStringPtr = COpaquePointer

typealias GDExtensionUninitializedStringPtrVar = CPointerVarOf<GDExtensionUninitializedStringPtr>

typealias GDExtensionUninitializedStringPtr = COpaquePointer

typealias GDExtensionObjectPtrVar = CPointerVarOf<GDExtensionObjectPtr>

typealias GDExtensionObjectPtr = COpaquePointer

typealias GDExtensionConstObjectPtrVar = CPointerVarOf<GDExtensionConstObjectPtr>

typealias GDExtensionConstObjectPtr = COpaquePointer

typealias GDExtensionUninitializedObjectPtrVar = CPointerVarOf<GDExtensionUninitializedObjectPtr>

typealias GDExtensionUninitializedObjectPtr = COpaquePointer

typealias GDExtensionTypePtrVar = CPointerVarOf<GDExtensionTypePtr>

typealias GDExtensionTypePtr = COpaquePointer

typealias GDExtensionConstTypePtrVar = CPointerVarOf<GDExtensionConstTypePtr>

typealias GDExtensionConstTypePtr = COpaquePointer

typealias GDExtensionUninitializedTypePtrVar = CPointerVarOf<GDExtensionUninitializedTypePtr>

typealias GDExtensionUninitializedTypePtr = COpaquePointer

typealias GDExtensionMethodBindPtrVar = CPointerVarOf<GDExtensionMethodBindPtr>

typealias GDExtensionMethodBindPtr = COpaquePointer

typealias GDExtensionIntVar = LongVarOf<GDExtensionInt>

typealias GDExtensionInt = int64_t

typealias GDExtensionBoolVar = UByteVarOf<GDExtensionBool>

typealias GDExtensionBool = uint8_t

typealias GDObjectInstanceIDVar = ULongVarOf<GDObjectInstanceID>

typealias GDObjectInstanceID = uint64_t

typealias GDExtensionRefPtrVar = CPointerVarOf<GDExtensionRefPtr>

typealias GDExtensionRefPtr = COpaquePointer

typealias GDExtensionConstRefPtrVar = CPointerVarOf<GDExtensionConstRefPtr>

typealias GDExtensionConstRefPtr = COpaquePointer

typealias GDExtensionVariantFromTypeConstructorFuncVar = CPointerVarOf<GDExtensionVariantFromTypeConstructorFunc>

typealias GDExtensionVariantFromTypeConstructorFunc = CPointer<CFunction<(GDExtensionUninitializedVariantPtr?, GDExtensionTypePtr?) -> Unit>>

typealias GDExtensionTypeFromVariantConstructorFuncVar = CPointerVarOf<GDExtensionTypeFromVariantConstructorFunc>

typealias GDExtensionTypeFromVariantConstructorFunc = CPointer<CFunction<(GDExtensionUninitializedTypePtr?, GDExtensionVariantPtr?) -> Unit>>

typealias GDExtensionVariantGetInternalPtrFuncVar = CPointerVarOf<GDExtensionVariantGetInternalPtrFunc>

typealias GDExtensionVariantGetInternalPtrFunc = CPointer<CFunction<(GDExtensionVariantPtr?) -> COpaquePointer?>>

typealias GDExtensionPtrOperatorEvaluatorVar = CPointerVarOf<GDExtensionPtrOperatorEvaluator>

typealias GDExtensionPtrOperatorEvaluator = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionConstTypePtr?, GDExtensionTypePtr?) -> Unit>>

typealias GDExtensionPtrBuiltInMethodVar = CPointerVarOf<GDExtensionPtrBuiltInMethod>

typealias GDExtensionPtrBuiltInMethod = CPointer<CFunction<(GDExtensionTypePtr?, CPointer<GDExtensionConstTypePtrVar>?, GDExtensionTypePtr?, int32_t) -> Unit>>

typealias GDExtensionPtrConstructorVar = CPointerVarOf<GDExtensionPtrConstructor>

typealias GDExtensionPtrConstructor = CPointer<CFunction<(GDExtensionUninitializedTypePtr?, CPointer<GDExtensionConstTypePtrVar>?) -> Unit>>

typealias GDExtensionPtrDestructorVar = CPointerVarOf<GDExtensionPtrDestructor>

typealias GDExtensionPtrDestructor = CPointer<CFunction<(GDExtensionTypePtr?) -> Unit>>

typealias GDExtensionPtrSetterVar = CPointerVarOf<GDExtensionPtrSetter>

typealias GDExtensionPtrSetter = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionConstTypePtr?) -> Unit>>

typealias GDExtensionPtrGetterVar = CPointerVarOf<GDExtensionPtrGetter>

typealias GDExtensionPtrGetter = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionTypePtr?) -> Unit>>

typealias GDExtensionPtrIndexedSetterVar = CPointerVarOf<GDExtensionPtrIndexedSetter>

typealias GDExtensionPtrIndexedSetter = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt, GDExtensionConstTypePtr?) -> Unit>>


typealias GDExtensionPtrIndexedGetterVar = CPointerVarOf<GDExtensionPtrIndexedGetter>

typealias GDExtensionPtrIndexedGetter = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt, GDExtensionTypePtr?) -> Unit>>

typealias GDExtensionPtrKeyedSetterVar = CPointerVarOf<GDExtensionPtrKeyedSetter>

typealias GDExtensionPtrKeyedSetter = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionConstTypePtr?, GDExtensionConstTypePtr?) -> Unit>>

typealias GDExtensionPtrKeyedGetterVar = CPointerVarOf<GDExtensionPtrKeyedGetter>

typealias GDExtensionPtrKeyedGetter = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionConstTypePtr?, GDExtensionTypePtr?) -> Unit>>

typealias GDExtensionPtrKeyedCheckerVar = CPointerVarOf<GDExtensionPtrKeyedChecker>

typealias GDExtensionPtrKeyedChecker = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionConstVariantPtr?) -> uint32_t>>

typealias GDExtensionPtrUtilityFunctionVar = CPointerVarOf<GDExtensionPtrUtilityFunction>

typealias GDExtensionPtrUtilityFunction = CPointer<CFunction<(GDExtensionTypePtr?, CPointer<GDExtensionConstTypePtrVar>?, int32_t) -> Unit>>

typealias GDExtensionClassConstructorVar = CPointerVarOf<GDExtensionClassConstructor>

typealias GDExtensionClassConstructor = COpaquePointer

typealias GDExtensionInstanceBindingCreateCallbackVar = CPointerVarOf<GDExtensionInstanceBindingCreateCallback>

typealias GDExtensionInstanceBindingCreateCallback = CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> COpaquePointer?>>

typealias GDExtensionInstanceBindingFreeCallbackVar = CPointerVarOf<GDExtensionInstanceBindingFreeCallback>

typealias GDExtensionInstanceBindingFreeCallback = CPointer<CFunction<(COpaquePointer?, COpaquePointer?, COpaquePointer?) -> Unit>>

typealias GDExtensionInstanceBindingReferenceCallbackVar = CPointerVarOf<GDExtensionInstanceBindingReferenceCallback>

typealias GDExtensionInstanceBindingReferenceCallback = CPointer<CFunction<(COpaquePointer?, COpaquePointer?, GDExtensionBool) -> GDExtensionBool>>

typealias GDExtensionClassInstancePtrVar = CPointerVarOf<GDExtensionClassInstancePtr>

typealias GDExtensionClassInstancePtr = COpaquePointer

typealias GDExtensionClassSetVar = CPointerVarOf<GDExtensionClassSet>

typealias GDExtensionClassSet = CPointer<CFunction<(GDExtensionClassInstancePtr?, GDExtensionBool>>

typealias GDExtensionClassGetVar = CPointerVarOf<GDExtensionClassGet>

typealias GDExtensionClassGet = CPointer<CFunction<(GDExtensionClassInstancePtr?, GDExtensionBool>>

typealias GDExtensionClassGetRIDVar = CPointerVarOf<GDExtensionClassGetRID>

typealias GDExtensionClassGetRID = CPointer<CFunction<(GDExtensionClassInstancePtr?) -> uint64_t>>

typealias GDExtensionClassGetPropertyListVar = CPointerVarOf<GDExtensionClassGetPropertyList>

typealias GDExtensionClassGetPropertyList = CPointer<CFunction<(GDExtensionClassInstancePtr?, CPointer<uint32_tVar>?) -> CPointer<GDExtensionPropertyInfo>?>>

typealias GDExtensionClassFreePropertyListVar = CPointerVarOf<GDExtensionClassFreePropertyList>

typealias GDExtensionClassFreePropertyList = Unit>>

typealias GDExtensionClassFreePropertyList2Var = CPointerVarOf<GDExtensionClassFreePropertyList2>

typealias GDExtensionClassFreePropertyList2 = Unit>>

typealias GDExtensionClassPropertyCanRevertVar = CPointerVarOf<GDExtensionClassPropertyCanRevert>

typealias GDExtensionClassPropertyCanRevert = CPointer<CFunction<(GDExtensionClassInstancePtr?, GDExtensionBool>>

typealias GDExtensionClassPropertyGetRevertVar = CPointerVarOf<GDExtensionClassPropertyGetRevert>

typealias GDExtensionClassPropertyGetRevert = CPointer<CFunction<(GDExtensionClassInstancePtr?, GDExtensionBool>>

typealias GDExtensionClassValidatePropertyVar = CPointerVarOf<GDExtensionClassValidateProperty>

typealias GDExtensionClassValidateProperty = GDExtensionBool>>

typealias GDExtensionClassNotificationVar = CPointerVarOf<GDExtensionClassNotification>

typealias GDExtensionClassNotification = Unit>>

typealias GDExtensionClassNotification2Var = CPointerVarOf<GDExtensionClassNotification2>

typealias GDExtensionClassNotification2 = CPointer<CFunction<(GDExtensionClassInstancePtr?, int32_t, GDExtensionBool) -> Unit>>

typealias GDExtensionClassToStringVar = CPointerVarOf<GDExtensionClassToString>

typealias GDExtensionClassToString = CPointer<CFunction<(GDExtensionClassInstancePtr?, CPointer<GDExtensionBoolVar>?, GDExtensionStringPtr?) -> Unit>>

typealias GDExtensionClassReferenceVar = CPointerVarOf<GDExtensionClassReference>

typealias GDExtensionClassReference = CPointer<CFunction<(GDExtensionClassInstancePtr?) -> Unit>>

typealias GDExtensionClassUnreferenceVar = CPointerVarOf<GDExtensionClassUnreference>

typealias GDExtensionClassUnreference = CPointer<CFunction<(GDExtensionClassInstancePtr?) -> Unit>>

typealias GDExtensionClassCallVirtualVar = CPointerVarOf<GDExtensionClassCallVirtual>

typealias GDExtensionClassCallVirtual = CPointer<CFunction<(GDExtensionClassInstancePtr?, CPointer<GDExtensionConstTypePtrVar>?, GDExtensionTypePtr?) -> Unit>>

typealias GDExtensionClassCreateInstanceVar = CPointerVarOf<GDExtensionClassCreateInstance>

typealias GDExtensionClassCreateInstance = CPointer<CFunction<(COpaquePointer?) -> GDExtensionObjectPtr?>>

typealias GDExtensionClassCreateInstance2Var = CPointerVarOf<GDExtensionClassCreateInstance2>

typealias GDExtensionClassCreateInstance2 = CPointer<CFunction<(COpaquePointer?, GDExtensionBool) -> GDExtensionObjectPtr?>>

typealias GDExtensionClassFreeInstanceVar = CPointerVarOf<GDExtensionClassFreeInstance>

typealias GDExtensionClassFreeInstance = CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?) -> Unit>>

typealias GDExtensionClassRecreateInstanceVar = CPointerVarOf<GDExtensionClassRecreateInstance>

typealias GDExtensionClassRecreateInstance = CPointer<CFunction<(COpaquePointer?, GDExtensionObjectPtr?) -> GDExtensionClassInstancePtr?>>

typealias GDExtensionClassGetVirtualVar = CPointerVarOf<GDExtensionClassGetVirtual>

typealias GDExtensionClassGetVirtual = CPointer<CFunction<(COpaquePointer?, GDExtensionClassCallVirtual?>>

typealias GDExtensionClassGetVirtual2Var = CPointerVarOf<GDExtensionClassGetVirtual2>

typealias GDExtensionClassGetVirtual2 = CPointer<CFunction<(COpaquePointer?, GDExtensionConstStringNamePtr?, uint32_t) -> GDExtensionClassCallVirtual?>>

typealias GDExtensionClassGetVirtualCallDataVar = CPointerVarOf<GDExtensionClassGetVirtualCallData>

typealias GDExtensionClassGetVirtualCallData = CPointer<CFunction<(COpaquePointer?, COpaquePointer?>>

typealias GDExtensionClassGetVirtualCallData2Var = CPointerVarOf<GDExtensionClassGetVirtualCallData2>

typealias GDExtensionClassGetVirtualCallData2 = >>

typealias GDExtensionClassCallVirtualWithDataVar = CPointerVarOf<GDExtensionClassCallVirtualWithData>

typealias GDExtensionClassCallVirtualWithData = CPointer<CFunction<(GDExtensionClassInstancePtr?, GDExtensionConstStringNamePtr?, COpaquePointer?, CPointer<GDExtensionConstTypePtrVar>?, GDExtensionTypePtr?) -> Unit>>

typealias GDExtensionClassCreationInfo5 = GDExtensionClassCreationInfo4

typealias GDExtensionClassLibraryPtrVar = CPointerVarOf<GDExtensionClassLibraryPtr>

typealias GDExtensionClassLibraryPtr = COpaquePointer

typealias GDExtensionEditorGetClassesUsedCallbackVar = CPointerVarOf<GDExtensionEditorGetClassesUsedCallback>

typealias GDExtensionEditorGetClassesUsedCallback = CPointer<CFunction<(GDExtensionTypePtr?) -> Unit>>

typealias GDExtensionClassMethodCallVar = CPointerVarOf<GDExtensionClassMethodCall>

typealias GDExtensionClassMethodCall = CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?, CPointer<GDExtensionConstVariantPtrVar>?, GDExtensionInt, GDExtensionVariantPtr?, CPointer<GDExtensionCallError>?) -> Unit>>

typealias GDExtensionClassMethodValidatedCallVar = CPointerVarOf<GDExtensionClassMethodValidatedCall>

typealias GDExtensionClassMethodValidatedCall = CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?, CPointer<GDExtensionConstVariantPtrVar>?, GDExtensionVariantPtr?) -> Unit>>

typealias GDExtensionClassMethodPtrCallVar = CPointerVarOf<GDExtensionClassMethodPtrCall>

typealias GDExtensionClassMethodPtrCall = CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?, CPointer<GDExtensionConstTypePtrVar>?, GDExtensionTypePtr?) -> Unit>>

typealias GDExtensionCallableCustomCallVar = CPointerVarOf<GDExtensionCallableCustomCall>

typealias GDExtensionCallableCustomCall = CPointer<CFunction<(COpaquePointer?, CPointer<GDExtensionConstVariantPtrVar>?, GDExtensionInt, GDExtensionVariantPtr?, CPointer<GDExtensionCallError>?) -> Unit>>

typealias GDExtensionCallableCustomIsValidVar = CPointerVarOf<GDExtensionCallableCustomIsValid>

typealias GDExtensionCallableCustomIsValid = CPointer<CFunction<(COpaquePointer?) -> GDExtensionBool>>

typealias GDExtensionCallableCustomFreeVar = CPointerVarOf<GDExtensionCallableCustomFree>

typealias GDExtensionCallableCustomFree = CPointer<CFunction<(COpaquePointer?) -> Unit>>

typealias GDExtensionCallableCustomHashVar = CPointerVarOf<GDExtensionCallableCustomHash>

typealias GDExtensionCallableCustomHash = CPointer<CFunction<(COpaquePointer?) -> uint32_t>>

typealias GDExtensionCallableCustomEqualVar = CPointerVarOf<GDExtensionCallableCustomEqual>

typealias GDExtensionCallableCustomEqual = CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> GDExtensionBool>>

typealias GDExtensionCallableCustomLessThanVar = CPointerVarOf<GDExtensionCallableCustomLessThan>

typealias GDExtensionCallableCustomLessThan = CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> GDExtensionBool>>

typealias GDExtensionCallableCustomToStringVar = CPointerVarOf<GDExtensionCallableCustomToString>

typealias GDExtensionCallableCustomToString = CPointer<CFunction<(COpaquePointer?, CPointer<GDExtensionBoolVar>?, GDExtensionStringPtr?) -> Unit>>

typealias GDExtensionCallableCustomGetArgumentCountVar = CPointerVarOf<GDExtensionCallableCustomGetArgumentCount>

typealias GDExtensionCallableCustomGetArgumentCount = CPointer<CFunction<(COpaquePointer?, CPointer<GDExtensionBoolVar>?) -> GDExtensionInt>>

typealias GDExtensionScriptInstanceDataPtrVar = CPointerVarOf<GDExtensionScriptInstanceDataPtr>

typealias GDExtensionScriptInstanceDataPtr = COpaquePointer

typealias GDExtensionScriptInstanceSetVar = CPointerVarOf<GDExtensionScriptInstanceSet>

typealias GDExtensionScriptInstanceSet = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionBool>>

typealias GDExtensionScriptInstanceGetVar = CPointerVarOf<GDExtensionScriptInstanceGet>

typealias GDExtensionScriptInstanceGet = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionBool>>

typealias GDExtensionScriptInstanceGetPropertyListVar = CPointerVarOf<GDExtensionScriptInstanceGetPropertyList>

typealias GDExtensionScriptInstanceGetPropertyList = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<uint32_tVar>?) -> CPointer<GDExtensionPropertyInfo>?>>

typealias GDExtensionScriptInstanceFreePropertyListVar = CPointerVarOf<GDExtensionScriptInstanceFreePropertyList>

typealias GDExtensionScriptInstanceFreePropertyList = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> Unit>>

typealias GDExtensionScriptInstanceFreePropertyList2Var = CPointerVarOf<GDExtensionScriptInstanceFreePropertyList2>

typealias GDExtensionScriptInstanceFreePropertyList2 = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?, uint32_t) -> Unit>>

typealias GDExtensionScriptInstanceGetClassCategoryVar = CPointerVarOf<GDExtensionScriptInstanceGetClassCategory>

typealias GDExtensionScriptInstanceGetClassCategory = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionBool>>

typealias GDExtensionScriptInstanceGetPropertyTypeVar = CPointerVarOf<GDExtensionScriptInstanceGetPropertyType>

typealias GDExtensionScriptInstanceGetPropertyType = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionBoolVar>?) -> GDExtensionVariantType>>

typealias GDExtensionScriptInstanceValidatePropertyVar = CPointerVarOf<GDExtensionScriptInstanceValidateProperty>

typealias GDExtensionScriptInstanceValidateProperty = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionBool>>

typealias GDExtensionScriptInstancePropertyCanRevertVar = CPointerVarOf<GDExtensionScriptInstancePropertyCanRevert>

typealias GDExtensionScriptInstancePropertyCanRevert = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionBool>>

typealias GDExtensionScriptInstancePropertyGetRevertVar = CPointerVarOf<GDExtensionScriptInstancePropertyGetRevert>

typealias GDExtensionScriptInstancePropertyGetRevert = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionBool>>

typealias GDExtensionScriptInstanceGetOwnerVar = CPointerVarOf<GDExtensionScriptInstanceGetOwner>

typealias GDExtensionScriptInstanceGetOwner = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?>>

typealias GDExtensionScriptInstancePropertyStateAddVar = CPointerVarOf<GDExtensionScriptInstancePropertyStateAdd>

typealias GDExtensionScriptInstancePropertyStateAdd = CPointer<CFunction<(GDExtensionConstStringNamePtr?, GDExtensionConstVariantPtr?, COpaquePointer?) -> Unit>>

typealias GDExtensionScriptInstanceGetPropertyStateVar = CPointerVarOf<GDExtensionScriptInstanceGetPropertyState>

typealias GDExtensionScriptInstanceGetPropertyState = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyStateAdd?, COpaquePointer?) -> Unit>>

typealias GDExtensionScriptInstanceGetMethodListVar = CPointerVarOf<GDExtensionScriptInstanceGetMethodList>

typealias GDExtensionScriptInstanceGetMethodList = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<uint32_tVar>?) -> CPointer<GDExtensionMethodInfo>?>>

typealias GDExtensionScriptInstanceFreeMethodListVar = CPointerVarOf<GDExtensionScriptInstanceFreeMethodList>

typealias GDExtensionScriptInstanceFreeMethodList = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionMethodInfo>?) -> Unit>>

typealias GDExtensionScriptInstanceFreeMethodList2Var = CPointerVarOf<GDExtensionScriptInstanceFreeMethodList2>

typealias GDExtensionScriptInstanceFreeMethodList2 = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionMethodInfo>?, uint32_t) -> Unit>>

typealias GDExtensionScriptInstanceHasMethodVar = CPointerVarOf<GDExtensionScriptInstanceHasMethod>

typealias GDExtensionScriptInstanceHasMethod = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionBool>>

typealias GDExtensionScriptInstanceGetMethodArgumentCountVar = CPointerVarOf<GDExtensionScriptInstanceGetMethodArgumentCount>

typealias GDExtensionScriptInstanceGetMethodArgumentCount = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionBoolVar>?) -> GDExtensionInt>>

typealias GDExtensionScriptInstanceCallVar = CPointerVarOf<GDExtensionScriptInstanceCall>

typealias GDExtensionScriptInstanceCall = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionConstVariantPtrVar>?, GDExtensionInt, GDExtensionVariantPtr?, CPointer<GDExtensionCallError>?) -> Unit>>

typealias GDExtensionScriptInstanceNotificationVar = CPointerVarOf<GDExtensionScriptInstanceNotification>

typealias GDExtensionScriptInstanceNotification = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, int32_t) -> Unit>>

typealias GDExtensionScriptInstanceNotification2Var = CPointerVarOf<GDExtensionScriptInstanceNotification2>

typealias GDExtensionScriptInstanceNotification2 = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, int32_t, GDExtensionBool) -> Unit>>

typealias GDExtensionScriptInstanceToStringVar = CPointerVarOf<GDExtensionScriptInstanceToString>

typealias GDExtensionScriptInstanceToString = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionBoolVar>?, GDExtensionStringPtr?) -> Unit>>

typealias GDExtensionScriptInstanceRefCountIncrementedVar = CPointerVarOf<GDExtensionScriptInstanceRefCountIncremented>

typealias GDExtensionScriptInstanceRefCountIncremented = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> Unit>>

typealias GDExtensionScriptInstanceRefCountDecrementedVar = CPointerVarOf<GDExtensionScriptInstanceRefCountDecremented>

typealias GDExtensionScriptInstanceRefCountDecremented = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionBool>>

typealias GDExtensionScriptInstanceGetScriptVar = CPointerVarOf<GDExtensionScriptInstanceGetScript>

typealias GDExtensionScriptInstanceGetScript = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?>>

typealias GDExtensionScriptInstanceIsPlaceholderVar = CPointerVarOf<GDExtensionScriptInstanceIsPlaceholder>

typealias GDExtensionScriptInstanceIsPlaceholder = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionBool>>

typealias GDExtensionScriptLanguagePtrVar = CPointerVarOf<GDExtensionScriptLanguagePtr>

typealias GDExtensionScriptLanguagePtr = COpaquePointer

typealias GDExtensionScriptInstanceGetLanguageVar = CPointerVarOf<GDExtensionScriptInstanceGetLanguage>

typealias GDExtensionScriptInstanceGetLanguage = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptLanguagePtr?>>

typealias GDExtensionScriptInstanceFreeVar = CPointerVarOf<GDExtensionScriptInstanceFree>

typealias GDExtensionScriptInstanceFree = CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> Unit>>

typealias GDExtensionScriptInstancePtrVar = CPointerVarOf<GDExtensionScriptInstancePtr>

typealias GDExtensionScriptInstancePtr = COpaquePointer

typealias GDExtensionWorkerThreadPoolGroupTaskVar = CPointerVarOf<GDExtensionWorkerThreadPoolGroupTask>

typealias GDExtensionWorkerThreadPoolGroupTask = CPointer<CFunction<(COpaquePointer?, uint32_t) -> Unit>>

typealias GDExtensionWorkerThreadPoolTaskVar = CPointerVarOf<GDExtensionWorkerThreadPoolTask>

typealias GDExtensionWorkerThreadPoolTask = CPointer<CFunction<(COpaquePointer?) -> Unit>>

typealias GDExtensionInitializeCallbackVar = CPointerVarOf<GDExtensionInitializeCallback>

typealias GDExtensionInitializeCallback = CPointer<CFunction<(COpaquePointer?, GDExtensionInitializationLevel) -> Unit>>

typealias GDExtensionDeinitializeCallbackVar = CPointerVarOf<GDExtensionDeinitializeCallback>

typealias GDExtensionDeinitializeCallback = CPointer<CFunction<(COpaquePointer?, GDExtensionInitializationLevel) -> Unit>>

typealias GDExtensionInterfaceFunctionPtrVar = CPointerVarOf<GDExtensionInterfaceFunctionPtr>

typealias GDExtensionInterfaceFunctionPtr = COpaquePointer

typealias GDExtensionInterfaceGetProcAddressVar = CPointerVarOf<GDExtensionInterfaceGetProcAddress>

typealias GDExtensionInterfaceGetProcAddress = CPointer<CFunction<(CPointer<ByteVar>?) -> GDExtensionInterfaceFunctionPtr?>>

typealias GDExtensionInitializationFunctionVar = CPointerVarOf<GDExtensionInitializationFunction>

typealias GDExtensionInitializationFunction = CPointer<CFunction<(GDExtensionInterfaceGetProcAddress?, GDExtensionClassLibraryPtr?, CPointer<GDExtensionInitialization>?) -> GDExtensionBool>>

typealias GDExtensionMainLoopStartupCallbackVar = CPointerVarOf<GDExtensionMainLoopStartupCallback>

typealias GDExtensionMainLoopStartupCallback = COpaquePointer

typealias GDExtensionMainLoopShutdownCallbackVar = CPointerVarOf<GDExtensionMainLoopShutdownCallback>

typealias GDExtensionMainLoopShutdownCallback = COpaquePointer

typealias GDExtensionMainLoopFrameCallbackVar = CPointerVarOf<GDExtensionMainLoopFrameCallback>

typealias GDExtensionMainLoopFrameCallback = COpaquePointer

typealias GDExtensionInterfaceGetGodotVersionVar = CPointerVarOf<GDExtensionInterfaceGetGodotVersion>

typealias GDExtensionInterfaceGetGodotVersion = CPointer<CFunction<(CPointer<GDExtensionGodotVersion>?) -> Unit>>

typealias GDExtensionInterfaceGetGodotVersion2Var = CPointerVarOf<GDExtensionInterfaceGetGodotVersion2>

typealias GDExtensionInterfaceGetGodotVersion2 = CPointer<CFunction<(CPointer<GDExtensionGodotVersion2>?) -> Unit>>

typealias GDExtensionInterfaceMemAllocVar = CPointerVarOf<GDExtensionInterfaceMemAlloc>

typealias GDExtensionInterfaceMemAlloc = CPointer<CFunction<(size_t) -> COpaquePointer?>>

typealias GDExtensionInterfaceMemReallocVar = CPointerVarOf<GDExtensionInterfaceMemRealloc>

typealias GDExtensionInterfaceMemRealloc = CPointer<CFunction<(COpaquePointer?, size_t) -> COpaquePointer?>>

typealias GDExtensionInterfaceMemFreeVar = CPointerVarOf<GDExtensionInterfaceMemFree>

typealias GDExtensionInterfaceMemFree = CPointer<CFunction<(COpaquePointer?) -> Unit>>

typealias GDExtensionInterfaceMemAlloc2Var = CPointerVarOf<GDExtensionInterfaceMemAlloc2>

typealias GDExtensionInterfaceMemAlloc2 = CPointer<CFunction<(size_t, GDExtensionBool) -> COpaquePointer?>>

typealias GDExtensionInterfaceMemRealloc2Var = CPointerVarOf<GDExtensionInterfaceMemRealloc2>

typealias GDExtensionInterfaceMemRealloc2 = CPointer<CFunction<(COpaquePointer?, size_t, GDExtensionBool) -> COpaquePointer?>>

typealias GDExtensionInterfaceMemFree2Var = CPointerVarOf<GDExtensionInterfaceMemFree2>

typealias GDExtensionInterfaceMemFree2 = CPointer<CFunction<(COpaquePointer?, GDExtensionBool) -> Unit>>

typealias GDExtensionInterfacePrintErrorVar = CPointerVarOf<GDExtensionInterfacePrintError>

typealias GDExtensionInterfacePrintError = CPointer<CFunction<(CPointer<ByteVar>?, CPointer<ByteVar>?, CPointer<ByteVar>?, int32_t, GDExtensionBool) -> Unit>>

typealias GDExtensionInterfacePrintErrorWithMessageVar = CPointerVarOf<GDExtensionInterfacePrintErrorWithMessage>

typealias GDExtensionInterfacePrintErrorWithMessage = CPointer<CFunction<(CPointer<ByteVar>?, CPointer<ByteVar>?, CPointer<ByteVar>?, CPointer<ByteVar>?, int32_t, GDExtensionBool) -> Unit>>

typealias GDExtensionInterfacePrintWarningVar = CPointerVarOf<GDExtensionInterfacePrintWarning>

typealias GDExtensionInterfacePrintWarning = CPointer<CFunction<(CPointer<ByteVar>?, CPointer<ByteVar>?, CPointer<ByteVar>?, int32_t, GDExtensionBool) -> Unit>>

typealias GDExtensionInterfacePrintWarningWithMessageVar = CPointerVarOf<GDExtensionInterfacePrintWarningWithMessage>

typealias GDExtensionInterfacePrintWarningWithMessage = CPointer<CFunction<(CPointer<ByteVar>?, CPointer<ByteVar>?, CPointer<ByteVar>?, CPointer<ByteVar>?, int32_t, GDExtensionBool) -> Unit>>

typealias GDExtensionInterfacePrintScriptErrorVar = CPointerVarOf<GDExtensionInterfacePrintScriptError>

typealias GDExtensionInterfacePrintScriptError = CPointer<CFunction<(CPointer<ByteVar>?, CPointer<ByteVar>?, CPointer<ByteVar>?, int32_t, GDExtensionBool) -> Unit>>

typealias GDExtensionInterfacePrintScriptErrorWithMessageVar = CPointerVarOf<GDExtensionInterfacePrintScriptErrorWithMessage>

typealias GDExtensionInterfacePrintScriptErrorWithMessage = CPointer<CFunction<(CPointer<ByteVar>?, CPointer<ByteVar>?, CPointer<ByteVar>?, CPointer<ByteVar>?, int32_t, GDExtensionBool) -> Unit>>

typealias GDExtensionInterfaceGetNativeStructSizeVar = CPointerVarOf<GDExtensionInterfaceGetNativeStructSize>

typealias GDExtensionInterfaceGetNativeStructSize = CPointer<CFunction<(uint64_t>>

typealias GDExtensionInterfaceVariantNewCopyVar = CPointerVarOf<GDExtensionInterfaceVariantNewCopy>

typealias GDExtensionInterfaceVariantNewCopy = CPointer<CFunction<(GDExtensionUninitializedVariantPtr?, GDExtensionConstVariantPtr?) -> Unit>>

typealias GDExtensionInterfaceVariantNewNilVar = CPointerVarOf<GDExtensionInterfaceVariantNewNil>

typealias GDExtensionInterfaceVariantNewNil = CPointer<CFunction<(GDExtensionUninitializedVariantPtr?) -> Unit>>

typealias GDExtensionInterfaceVariantDestroyVar = CPointerVarOf<GDExtensionInterfaceVariantDestroy>

typealias GDExtensionInterfaceVariantDestroy = CPointer<CFunction<(GDExtensionVariantPtr?) -> Unit>>

typealias GDExtensionInterfaceVariantCallVar = CPointerVarOf<GDExtensionInterfaceVariantCall>

typealias GDExtensionInterfaceVariantCall = CPointer<CFunction<(GDExtensionVariantPtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionConstVariantPtrVar>?, GDExtensionInt, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionCallError>?) -> Unit>>

typealias GDExtensionInterfaceVariantCallStaticVar = CPointerVarOf<GDExtensionInterfaceVariantCallStatic>

typealias GDExtensionInterfaceVariantCallStatic = CPointer<CFunction<(GDExtensionVariantType, GDExtensionConstStringNamePtr?, CPointer<GDExtensionConstVariantPtrVar>?, GDExtensionInt, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionCallError>?) -> Unit>>

typealias GDExtensionInterfaceVariantEvaluateVar = CPointerVarOf<GDExtensionInterfaceVariantEvaluate>

typealias GDExtensionInterfaceVariantEvaluate = CPointer<CFunction<(GDExtensionVariantOperator, GDExtensionConstVariantPtr?, GDExtensionConstVariantPtr?, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionBoolVar>?) -> Unit>>

typealias GDExtensionInterfaceVariantSetVar = CPointerVarOf<GDExtensionInterfaceVariantSet>

typealias GDExtensionInterfaceVariantSet = CPointer<CFunction<(GDExtensionVariantPtr?, GDExtensionConstVariantPtr?, GDExtensionConstVariantPtr?, CPointer<GDExtensionBoolVar>?) -> Unit>>

typealias GDExtensionInterfaceVariantSetNamedVar = CPointerVarOf<GDExtensionInterfaceVariantSetNamed>

typealias GDExtensionInterfaceVariantSetNamed = CPointer<CFunction<(GDExtensionVariantPtr?, GDExtensionConstStringNamePtr?, GDExtensionConstVariantPtr?, CPointer<GDExtensionBoolVar>?) -> Unit>>

typealias GDExtensionInterfaceVariantSetKeyedVar = CPointerVarOf<GDExtensionInterfaceVariantSetKeyed>

typealias GDExtensionInterfaceVariantSetKeyed = CPointer<CFunction<(GDExtensionVariantPtr?, GDExtensionConstVariantPtr?, GDExtensionConstVariantPtr?, CPointer<GDExtensionBoolVar>?) -> Unit>>

typealias GDExtensionInterfaceVariantSetIndexedVar = CPointerVarOf<GDExtensionInterfaceVariantSetIndexed>

typealias GDExtensionInterfaceVariantSetIndexed = CPointer<CFunction<(GDExtensionVariantPtr?, GDExtensionInt, GDExtensionConstVariantPtr?, CPointer<GDExtensionBoolVar>?, CPointer<GDExtensionBoolVar>?) -> Unit>>

typealias GDExtensionInterfaceVariantGetVar = CPointerVarOf<GDExtensionInterfaceVariantGet>

typealias GDExtensionInterfaceVariantGet = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionConstVariantPtr?, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionBoolVar>?) -> Unit>>

typealias GDExtensionInterfaceVariantGetNamedVar = CPointerVarOf<GDExtensionInterfaceVariantGetNamed>

typealias GDExtensionInterfaceVariantGetNamed = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionConstStringNamePtr?, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionBoolVar>?) -> Unit>>

typealias GDExtensionInterfaceVariantGetKeyedVar = CPointerVarOf<GDExtensionInterfaceVariantGetKeyed>

typealias GDExtensionInterfaceVariantGetKeyed = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionConstVariantPtr?, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionBoolVar>?) -> Unit>>

typealias GDExtensionInterfaceVariantGetIndexedVar = CPointerVarOf<GDExtensionInterfaceVariantGetIndexed>

typealias GDExtensionInterfaceVariantGetIndexed = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionInt, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionBoolVar>?, CPointer<GDExtensionBoolVar>?) -> Unit>>

typealias GDExtensionInterfaceVariantIterInitVar = CPointerVarOf<GDExtensionInterfaceVariantIterInit>

typealias GDExtensionInterfaceVariantIterInit = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionBoolVar>?) -> GDExtensionBool>>

typealias GDExtensionInterfaceVariantIterNextVar = CPointerVarOf<GDExtensionInterfaceVariantIterNext>

typealias GDExtensionInterfaceVariantIterNext = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionVariantPtr?, CPointer<GDExtensionBoolVar>?) -> GDExtensionBool>>

typealias GDExtensionInterfaceVariantIterGetVar = CPointerVarOf<GDExtensionInterfaceVariantIterGet>

typealias GDExtensionInterfaceVariantIterGet = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionVariantPtr?, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionBoolVar>?) -> Unit>>

typealias GDExtensionInterfaceVariantHashVar = CPointerVarOf<GDExtensionInterfaceVariantHash>

typealias GDExtensionInterfaceVariantHash = CPointer<CFunction<(GDExtensionConstVariantPtr?) -> GDExtensionInt>>

typealias GDExtensionInterfaceVariantRecursiveHashVar = CPointerVarOf<GDExtensionInterfaceVariantRecursiveHash>

typealias GDExtensionInterfaceVariantRecursiveHash = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionInt) -> GDExtensionInt>>

typealias GDExtensionInterfaceVariantHashCompareVar = CPointerVarOf<GDExtensionInterfaceVariantHashCompare>

typealias GDExtensionInterfaceVariantHashCompare = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionConstVariantPtr?) -> GDExtensionBool>>

typealias GDExtensionInterfaceVariantBooleanizeVar = CPointerVarOf<GDExtensionInterfaceVariantBooleanize>

typealias GDExtensionInterfaceVariantBooleanize = CPointer<CFunction<(GDExtensionConstVariantPtr?) -> GDExtensionBool>>

typealias GDExtensionInterfaceVariantDuplicateVar = CPointerVarOf<GDExtensionInterfaceVariantDuplicate>

typealias GDExtensionInterfaceVariantDuplicate = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionVariantPtr?, GDExtensionBool) -> Unit>>

typealias GDExtensionInterfaceVariantStringifyVar = CPointerVarOf<GDExtensionInterfaceVariantStringify>

typealias GDExtensionInterfaceVariantStringify = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionStringPtr?) -> Unit>>

typealias GDExtensionInterfaceVariantGetTypeVar = CPointerVarOf<GDExtensionInterfaceVariantGetType>

typealias GDExtensionInterfaceVariantGetType = CPointer<CFunction<(GDExtensionConstVariantPtr?) -> GDExtensionVariantType>>

typealias GDExtensionInterfaceVariantHasMethodVar = CPointerVarOf<GDExtensionInterfaceVariantHasMethod>

typealias GDExtensionInterfaceVariantHasMethod = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionBool>>

typealias GDExtensionInterfaceVariantHasMemberVar = CPointerVarOf<GDExtensionInterfaceVariantHasMember>

typealias GDExtensionInterfaceVariantHasMember = CPointer<CFunction<(GDExtensionVariantType, GDExtensionBool>>

typealias GDExtensionInterfaceVariantHasKeyVar = CPointerVarOf<GDExtensionInterfaceVariantHasKey>

typealias GDExtensionInterfaceVariantHasKey = CPointer<CFunction<(GDExtensionConstVariantPtr?, GDExtensionConstVariantPtr?, CPointer<GDExtensionBoolVar>?) -> GDExtensionBool>>

typealias GDExtensionInterfaceVariantGetObjectInstanceIdVar = CPointerVarOf<GDExtensionInterfaceVariantGetObjectInstanceId>

typealias GDExtensionInterfaceVariantGetObjectInstanceId = CPointer<CFunction<(GDExtensionConstVariantPtr?) -> GDObjectInstanceID>>

typealias GDExtensionInterfaceVariantGetTypeNameVar = CPointerVarOf<GDExtensionInterfaceVariantGetTypeName>

typealias GDExtensionInterfaceVariantGetTypeName = CPointer<CFunction<(GDExtensionVariantType, GDExtensionUninitializedStringPtr?) -> Unit>>

typealias GDExtensionInterfaceVariantCanConvertVar = CPointerVarOf<GDExtensionInterfaceVariantCanConvert>

typealias GDExtensionInterfaceVariantCanConvert = CPointer<CFunction<(GDExtensionVariantType, GDExtensionVariantType) -> GDExtensionBool>>

typealias GDExtensionInterfaceVariantCanConvertStrictVar = CPointerVarOf<GDExtensionInterfaceVariantCanConvertStrict>

typealias GDExtensionInterfaceVariantCanConvertStrict = CPointer<CFunction<(GDExtensionVariantType, GDExtensionVariantType) -> GDExtensionBool>>

typealias GDExtensionInterfaceGetVariantFromTypeConstructorVar = CPointerVarOf<GDExtensionInterfaceGetVariantFromTypeConstructor>

typealias GDExtensionInterfaceGetVariantFromTypeConstructor = CPointer<CFunction<(GDExtensionVariantType) -> GDExtensionVariantFromTypeConstructorFunc?>>

typealias GDExtensionInterfaceGetVariantToTypeConstructorVar = CPointerVarOf<GDExtensionInterfaceGetVariantToTypeConstructor>

typealias GDExtensionInterfaceGetVariantToTypeConstructor = CPointer<CFunction<(GDExtensionVariantType) -> GDExtensionTypeFromVariantConstructorFunc?>>

typealias GDExtensionInterfaceGetVariantGetInternalPtrFuncVar = CPointerVarOf<GDExtensionInterfaceGetVariantGetInternalPtrFunc>

typealias GDExtensionInterfaceGetVariantGetInternalPtrFunc = CPointer<CFunction<(GDExtensionVariantType) -> GDExtensionVariantGetInternalPtrFunc?>>

typealias GDExtensionInterfaceVariantGetPtrOperatorEvaluatorVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrOperatorEvaluator>

typealias GDExtensionInterfaceVariantGetPtrOperatorEvaluator = CPointer<CFunction<(GDExtensionVariantOperator, GDExtensionVariantType, GDExtensionVariantType) -> GDExtensionPtrOperatorEvaluator?>>

typealias GDExtensionInterfaceVariantGetPtrBuiltinMethodVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrBuiltinMethod>

typealias GDExtensionInterfaceVariantGetPtrBuiltinMethod = CPointer<CFunction<(GDExtensionVariantType, GDExtensionConstStringNamePtr?, GDExtensionInt) -> GDExtensionPtrBuiltInMethod?>>

typealias GDExtensionInterfaceVariantGetPtrConstructorVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrConstructor>

typealias GDExtensionInterfaceVariantGetPtrConstructor = CPointer<CFunction<(GDExtensionVariantType, int32_t) -> GDExtensionPtrConstructor?>>

typealias GDExtensionInterfaceVariantGetPtrDestructorVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrDestructor>

typealias GDExtensionInterfaceVariantGetPtrDestructor = CPointer<CFunction<(GDExtensionVariantType) -> GDExtensionPtrDestructor?>>

typealias GDExtensionInterfaceVariantConstructVar = CPointerVarOf<GDExtensionInterfaceVariantConstruct>

typealias GDExtensionInterfaceVariantConstruct = CPointer<CFunction<(GDExtensionVariantType, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionConstVariantPtrVar>?, int32_t, CPointer<GDExtensionCallError>?) -> Unit>>

typealias GDExtensionInterfaceVariantGetPtrSetterVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrSetter>

typealias GDExtensionInterfaceVariantGetPtrSetter = CPointer<CFunction<(GDExtensionVariantType, GDExtensionPtrSetter?>>

typealias GDExtensionInterfaceVariantGetPtrGetterVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrGetter>

typealias GDExtensionInterfaceVariantGetPtrGetter = CPointer<CFunction<(GDExtensionVariantType, GDExtensionPtrGetter?>>

typealias GDExtensionInterfaceVariantGetPtrIndexedSetterVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrIndexedSetter>

typealias GDExtensionInterfaceVariantGetPtrIndexedSetter = CPointer<CFunction<(GDExtensionVariantType) -> GDExtensionPtrIndexedSetter?>>

typealias GDExtensionInterfaceVariantGetPtrIndexedGetterVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrIndexedGetter>

typealias GDExtensionInterfaceVariantGetPtrIndexedGetter = CPointer<CFunction<(GDExtensionVariantType) -> GDExtensionPtrIndexedGetter?>>

typealias GDExtensionInterfaceVariantGetPtrKeyedSetterVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrKeyedSetter>

typealias GDExtensionInterfaceVariantGetPtrKeyedSetter = CPointer<CFunction<(GDExtensionVariantType) -> GDExtensionPtrKeyedSetter?>>

typealias GDExtensionInterfaceVariantGetPtrKeyedGetterVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrKeyedGetter>

typealias GDExtensionInterfaceVariantGetPtrKeyedGetter = CPointer<CFunction<(GDExtensionVariantType) -> GDExtensionPtrKeyedGetter?>>

typealias GDExtensionInterfaceVariantGetPtrKeyedCheckerVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrKeyedChecker>

typealias GDExtensionInterfaceVariantGetPtrKeyedChecker = CPointer<CFunction<(GDExtensionVariantType) -> GDExtensionPtrKeyedChecker?>>

typealias GDExtensionInterfaceVariantGetConstantValueVar = CPointerVarOf<GDExtensionInterfaceVariantGetConstantValue>

typealias GDExtensionInterfaceVariantGetConstantValue = CPointer<CFunction<(GDExtensionVariantType, GDExtensionConstStringNamePtr?, GDExtensionUninitializedVariantPtr?) -> Unit>>

typealias GDExtensionInterfaceVariantGetPtrUtilityFunctionVar = CPointerVarOf<GDExtensionInterfaceVariantGetPtrUtilityFunction>

typealias GDExtensionInterfaceVariantGetPtrUtilityFunction = CPointer<CFunction<(GDExtensionConstStringNamePtr?, GDExtensionInt) -> GDExtensionPtrUtilityFunction?>>

typealias GDExtensionInterfaceStringNewWithLatin1CharsVar = CPointerVarOf<GDExtensionInterfaceStringNewWithLatin1Chars>

typealias GDExtensionInterfaceStringNewWithLatin1Chars = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<ByteVar>?) -> Unit>>

typealias GDExtensionInterfaceStringNewWithUtf8CharsVar = CPointerVarOf<GDExtensionInterfaceStringNewWithUtf8Chars>

typealias GDExtensionInterfaceStringNewWithUtf8Chars = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<ByteVar>?) -> Unit>>

typealias GDExtensionInterfaceStringNewWithUtf16CharsVar = CPointerVarOf<GDExtensionInterfaceStringNewWithUtf16Chars>

typealias GDExtensionInterfaceStringNewWithUtf16Chars = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<char16_tVar>?) -> Unit>>

typealias GDExtensionInterfaceStringNewWithUtf32CharsVar = CPointerVarOf<GDExtensionInterfaceStringNewWithUtf32Chars>

typealias GDExtensionInterfaceStringNewWithUtf32Chars = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<char32_tVar>?) -> Unit>>

typealias GDExtensionInterfaceStringNewWithWideCharsVar = CPointerVarOf<GDExtensionInterfaceStringNewWithWideChars>

typealias GDExtensionInterfaceStringNewWithWideChars = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<wchar_tVar>?) -> Unit>>

typealias GDExtensionInterfaceStringNewWithLatin1CharsAndLenVar = CPointerVarOf<GDExtensionInterfaceStringNewWithLatin1CharsAndLen>

typealias GDExtensionInterfaceStringNewWithLatin1CharsAndLen = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<ByteVar>?, GDExtensionInt) -> Unit>>

typealias GDExtensionInterfaceStringNewWithUtf8CharsAndLenVar = CPointerVarOf<GDExtensionInterfaceStringNewWithUtf8CharsAndLen>

typealias GDExtensionInterfaceStringNewWithUtf8CharsAndLen = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<ByteVar>?, GDExtensionInt) -> Unit>>

typealias GDExtensionInterfaceStringNewWithUtf8CharsAndLen2Var = CPointerVarOf<GDExtensionInterfaceStringNewWithUtf8CharsAndLen2>

typealias GDExtensionInterfaceStringNewWithUtf8CharsAndLen2 = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<ByteVar>?, GDExtensionInt) -> GDExtensionInt>>

typealias GDExtensionInterfaceStringNewWithUtf16CharsAndLenVar = CPointerVarOf<GDExtensionInterfaceStringNewWithUtf16CharsAndLen>

typealias GDExtensionInterfaceStringNewWithUtf16CharsAndLen = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<char16_tVar>?, GDExtensionInt) -> Unit>>

typealias GDExtensionInterfaceStringNewWithUtf16CharsAndLen2Var = CPointerVarOf<GDExtensionInterfaceStringNewWithUtf16CharsAndLen2>

typealias GDExtensionInterfaceStringNewWithUtf16CharsAndLen2 = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<char16_tVar>?, GDExtensionInt, GDExtensionBool) -> GDExtensionInt>>

typealias GDExtensionInterfaceStringNewWithUtf32CharsAndLenVar = CPointerVarOf<GDExtensionInterfaceStringNewWithUtf32CharsAndLen>

typealias GDExtensionInterfaceStringNewWithUtf32CharsAndLen = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<char32_tVar>?, GDExtensionInt) -> Unit>>

typealias GDExtensionInterfaceStringNewWithWideCharsAndLenVar = CPointerVarOf<GDExtensionInterfaceStringNewWithWideCharsAndLen>

typealias GDExtensionInterfaceStringNewWithWideCharsAndLen = CPointer<CFunction<(GDExtensionUninitializedStringPtr?, CPointer<wchar_tVar>?, GDExtensionInt) -> Unit>>

typealias GDExtensionInterfaceStringToLatin1CharsVar = CPointerVarOf<GDExtensionInterfaceStringToLatin1Chars>

typealias GDExtensionInterfaceStringToLatin1Chars = CPointer<CFunction<(GDExtensionConstStringPtr?, CPointer<ByteVar>?, GDExtensionInt) -> GDExtensionInt>>

typealias GDExtensionInterfaceStringToUtf8CharsVar = CPointerVarOf<GDExtensionInterfaceStringToUtf8Chars>

typealias GDExtensionInterfaceStringToUtf8Chars = CPointer<CFunction<(GDExtensionConstStringPtr?, CPointer<ByteVar>?, GDExtensionInt) -> GDExtensionInt>>

typealias GDExtensionInterfaceStringToUtf16CharsVar = CPointerVarOf<GDExtensionInterfaceStringToUtf16Chars>

typealias GDExtensionInterfaceStringToUtf16Chars = CPointer<CFunction<(GDExtensionConstStringPtr?, CPointer<char16_tVar>?, GDExtensionInt) -> GDExtensionInt>>

typealias GDExtensionInterfaceStringToUtf32CharsVar = CPointerVarOf<GDExtensionInterfaceStringToUtf32Chars>

typealias GDExtensionInterfaceStringToUtf32Chars = CPointer<CFunction<(GDExtensionConstStringPtr?, CPointer<char32_tVar>?, GDExtensionInt) -> GDExtensionInt>>

typealias GDExtensionInterfaceStringToWideCharsVar = CPointerVarOf<GDExtensionInterfaceStringToWideChars>

typealias GDExtensionInterfaceStringToWideChars = CPointer<CFunction<(GDExtensionConstStringPtr?, CPointer<wchar_tVar>?, GDExtensionInt) -> GDExtensionInt>>

typealias GDExtensionInterfaceStringOperatorIndexVar = CPointerVarOf<GDExtensionInterfaceStringOperatorIndex>

typealias GDExtensionInterfaceStringOperatorIndex = CPointer<CFunction<(GDExtensionStringPtr?, GDExtensionInt) -> CPointer<char32_tVar>?>>

typealias GDExtensionInterfaceStringOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfaceStringOperatorIndexConst>

typealias GDExtensionInterfaceStringOperatorIndexConst = CPointer<CFunction<(GDExtensionConstStringPtr?, GDExtensionInt) -> CPointer<char32_tVar>?>>

typealias GDExtensionInterfaceStringOperatorPlusEqStringVar = CPointerVarOf<GDExtensionInterfaceStringOperatorPlusEqString>

typealias GDExtensionInterfaceStringOperatorPlusEqString = CPointer<CFunction<(GDExtensionStringPtr?, GDExtensionConstStringPtr?) -> Unit>>

typealias GDExtensionInterfaceStringOperatorPlusEqCharVar = CPointerVarOf<GDExtensionInterfaceStringOperatorPlusEqChar>

typealias GDExtensionInterfaceStringOperatorPlusEqChar = CPointer<CFunction<(GDExtensionStringPtr?, char32_t) -> Unit>>

typealias GDExtensionInterfaceStringOperatorPlusEqCstrVar = CPointerVarOf<GDExtensionInterfaceStringOperatorPlusEqCstr>

typealias GDExtensionInterfaceStringOperatorPlusEqCstr = CPointer<CFunction<(GDExtensionStringPtr?, CPointer<ByteVar>?) -> Unit>>

typealias GDExtensionInterfaceStringOperatorPlusEqWcstrVar = CPointerVarOf<GDExtensionInterfaceStringOperatorPlusEqWcstr>

typealias GDExtensionInterfaceStringOperatorPlusEqWcstr = CPointer<CFunction<(GDExtensionStringPtr?, CPointer<wchar_tVar>?) -> Unit>>

typealias GDExtensionInterfaceStringOperatorPlusEqC32strVar = CPointerVarOf<GDExtensionInterfaceStringOperatorPlusEqC32str>

typealias GDExtensionInterfaceStringOperatorPlusEqC32str = CPointer<CFunction<(GDExtensionStringPtr?, CPointer<char32_tVar>?) -> Unit>>

typealias GDExtensionInterfaceStringResizeVar = CPointerVarOf<GDExtensionInterfaceStringResize>

typealias GDExtensionInterfaceStringResize = CPointer<CFunction<(GDExtensionStringPtr?, GDExtensionInt) -> GDExtensionInt>>

typealias GDExtensionInterfaceStringNameNewWithLatin1CharsVar = CPointerVarOf<GDExtensionInterfaceStringNameNewWithLatin1Chars>

typealias GDExtensionInterfaceStringNameNewWithLatin1Chars = CPointer<CFunction<(GDExtensionUninitializedStringNamePtr?, CPointer<ByteVar>?, GDExtensionBool) -> Unit>>

typealias GDExtensionInterfaceStringNameNewWithUtf8CharsVar = CPointerVarOf<GDExtensionInterfaceStringNameNewWithUtf8Chars>

typealias GDExtensionInterfaceStringNameNewWithUtf8Chars = CPointer<CFunction<(GDExtensionUninitializedStringNamePtr?, CPointer<ByteVar>?) -> Unit>>

typealias GDExtensionInterfaceStringNameNewWithUtf8CharsAndLenVar = CPointerVarOf<GDExtensionInterfaceStringNameNewWithUtf8CharsAndLen>

typealias GDExtensionInterfaceStringNameNewWithUtf8CharsAndLen = CPointer<CFunction<(GDExtensionUninitializedStringNamePtr?, CPointer<ByteVar>?, GDExtensionInt) -> Unit>>

typealias GDExtensionInterfaceXmlParserOpenBufferVar = CPointerVarOf<GDExtensionInterfaceXmlParserOpenBuffer>

typealias GDExtensionInterfaceXmlParserOpenBuffer = CPointer<CFunction<(GDExtensionObjectPtr?, CPointer<uint8_tVar>?, size_t) -> GDExtensionInt>>

typealias GDExtensionInterfaceFileAccessStoreBufferVar = CPointerVarOf<GDExtensionInterfaceFileAccessStoreBuffer>

typealias GDExtensionInterfaceFileAccessStoreBuffer = CPointer<CFunction<(GDExtensionObjectPtr?, CPointer<uint8_tVar>?, uint64_t) -> Unit>>

typealias GDExtensionInterfaceFileAccessGetBufferVar = CPointerVarOf<GDExtensionInterfaceFileAccessGetBuffer>

typealias GDExtensionInterfaceFileAccessGetBuffer = CPointer<CFunction<(GDExtensionConstObjectPtr?, CPointer<uint8_tVar>?, uint64_t) -> uint64_t>>

typealias GDExtensionInterfaceImagePtrwVar = CPointerVarOf<GDExtensionInterfaceImagePtrw>

typealias GDExtensionInterfaceImagePtrw = CPointer<CFunction<(GDExtensionObjectPtr?) -> CPointer<uint8_tVar>?>>

typealias GDExtensionInterfaceImagePtrVar = CPointerVarOf<GDExtensionInterfaceImagePtr>

typealias GDExtensionInterfaceImagePtr = CPointer<CFunction<(GDExtensionObjectPtr?) -> CPointer<uint8_tVar>?>>

typealias GDExtensionInterfaceWorkerThreadPoolAddNativeGroupTaskVar = CPointerVarOf<GDExtensionInterfaceWorkerThreadPoolAddNativeGroupTask>

typealias GDExtensionInterfaceWorkerThreadPoolAddNativeGroupTask = CPointer<CFunction<(GDExtensionObjectPtr?, GDExtensionWorkerThreadPoolGroupTask?, COpaquePointer?, int32_t, int32_t, GDExtensionBool, GDExtensionConstStringPtr?) -> int64_t>>

typealias GDExtensionInterfaceWorkerThreadPoolAddNativeTaskVar = CPointerVarOf<GDExtensionInterfaceWorkerThreadPoolAddNativeTask>

typealias GDExtensionInterfaceWorkerThreadPoolAddNativeTask = CPointer<CFunction<(GDExtensionObjectPtr?, GDExtensionWorkerThreadPoolTask?, COpaquePointer?, GDExtensionBool, GDExtensionConstStringPtr?) -> int64_t>>

typealias GDExtensionInterfacePackedByteArrayOperatorIndexVar = CPointerVarOf<GDExtensionInterfacePackedByteArrayOperatorIndex>

typealias GDExtensionInterfacePackedByteArrayOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt) -> CPointer<uint8_tVar>?>>

typealias GDExtensionInterfacePackedByteArrayOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfacePackedByteArrayOperatorIndexConst>

typealias GDExtensionInterfacePackedByteArrayOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt) -> CPointer<uint8_tVar>?>>

typealias GDExtensionInterfacePackedFloat32ArrayOperatorIndexVar = CPointerVarOf<GDExtensionInterfacePackedFloat32ArrayOperatorIndex>

typealias GDExtensionInterfacePackedFloat32ArrayOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt) -> CPointer<FloatVar>?>>

typealias GDExtensionInterfacePackedFloat32ArrayOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfacePackedFloat32ArrayOperatorIndexConst>

typealias GDExtensionInterfacePackedFloat32ArrayOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt) -> CPointer<FloatVar>?>>

typealias GDExtensionInterfacePackedFloat64ArrayOperatorIndexVar = CPointerVarOf<GDExtensionInterfacePackedFloat64ArrayOperatorIndex>

typealias GDExtensionInterfacePackedFloat64ArrayOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt) -> CPointer<DoubleVar>?>>

typealias GDExtensionInterfacePackedFloat64ArrayOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfacePackedFloat64ArrayOperatorIndexConst>

typealias GDExtensionInterfacePackedFloat64ArrayOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt) -> CPointer<DoubleVar>?>>

typealias GDExtensionInterfacePackedInt32ArrayOperatorIndexVar = CPointerVarOf<GDExtensionInterfacePackedInt32ArrayOperatorIndex>

typealias GDExtensionInterfacePackedInt32ArrayOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt) -> CPointer<int32_tVar>?>>

typealias GDExtensionInterfacePackedInt32ArrayOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfacePackedInt32ArrayOperatorIndexConst>

typealias GDExtensionInterfacePackedInt32ArrayOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt) -> CPointer<int32_tVar>?>>

typealias GDExtensionInterfacePackedInt64ArrayOperatorIndexVar = CPointerVarOf<GDExtensionInterfacePackedInt64ArrayOperatorIndex>

typealias GDExtensionInterfacePackedInt64ArrayOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt) -> CPointer<int64_tVar>?>>

typealias GDExtensionInterfacePackedInt64ArrayOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfacePackedInt64ArrayOperatorIndexConst>

typealias GDExtensionInterfacePackedInt64ArrayOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt) -> CPointer<int64_tVar>?>>

typealias GDExtensionInterfacePackedStringArrayOperatorIndexVar = CPointerVarOf<GDExtensionInterfacePackedStringArrayOperatorIndex>

typealias GDExtensionInterfacePackedStringArrayOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt) -> GDExtensionStringPtr?>>

typealias GDExtensionInterfacePackedStringArrayOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfacePackedStringArrayOperatorIndexConst>

typealias GDExtensionInterfacePackedStringArrayOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt) -> GDExtensionStringPtr?>>

typealias GDExtensionInterfacePackedVector2ArrayOperatorIndexVar = CPointerVarOf<GDExtensionInterfacePackedVector2ArrayOperatorIndex>

typealias GDExtensionInterfacePackedVector2ArrayOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt) -> GDExtensionTypePtr?>>

typealias GDExtensionInterfacePackedVector2ArrayOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfacePackedVector2ArrayOperatorIndexConst>

typealias GDExtensionInterfacePackedVector2ArrayOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt) -> GDExtensionTypePtr?>>

typealias GDExtensionInterfacePackedVector3ArrayOperatorIndexVar = CPointerVarOf<GDExtensionInterfacePackedVector3ArrayOperatorIndex>

typealias GDExtensionInterfacePackedVector3ArrayOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt) -> GDExtensionTypePtr?>>

typealias GDExtensionInterfacePackedVector3ArrayOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfacePackedVector3ArrayOperatorIndexConst>

typealias GDExtensionInterfacePackedVector3ArrayOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt) -> GDExtensionTypePtr?>>

typealias GDExtensionInterfacePackedVector4ArrayOperatorIndexVar = CPointerVarOf<GDExtensionInterfacePackedVector4ArrayOperatorIndex>

typealias GDExtensionInterfacePackedVector4ArrayOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt) -> GDExtensionTypePtr?>>

typealias GDExtensionInterfacePackedVector4ArrayOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfacePackedVector4ArrayOperatorIndexConst>

typealias GDExtensionInterfacePackedVector4ArrayOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt) -> GDExtensionTypePtr?>>

typealias GDExtensionInterfacePackedColorArrayOperatorIndexVar = CPointerVarOf<GDExtensionInterfacePackedColorArrayOperatorIndex>

typealias GDExtensionInterfacePackedColorArrayOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt) -> GDExtensionTypePtr?>>

typealias GDExtensionInterfacePackedColorArrayOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfacePackedColorArrayOperatorIndexConst>

typealias GDExtensionInterfacePackedColorArrayOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt) -> GDExtensionTypePtr?>>

typealias GDExtensionInterfaceArrayOperatorIndexVar = CPointerVarOf<GDExtensionInterfaceArrayOperatorIndex>

typealias GDExtensionInterfaceArrayOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionInt) -> GDExtensionVariantPtr?>>

typealias GDExtensionInterfaceArrayOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfaceArrayOperatorIndexConst>

typealias GDExtensionInterfaceArrayOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionInt) -> GDExtensionVariantPtr?>>

typealias GDExtensionInterfaceArrayRefVar = CPointerVarOf<GDExtensionInterfaceArrayRef>

typealias GDExtensionInterfaceArrayRef = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionConstTypePtr?) -> Unit>>

typealias GDExtensionInterfaceArraySetTypedVar = CPointerVarOf<GDExtensionInterfaceArraySetTyped>

typealias GDExtensionInterfaceArraySetTyped = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionVariantType, Unit>>

typealias GDExtensionInterfaceDictionaryOperatorIndexVar = CPointerVarOf<GDExtensionInterfaceDictionaryOperatorIndex>

typealias GDExtensionInterfaceDictionaryOperatorIndex = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionConstVariantPtr?) -> GDExtensionVariantPtr?>>

typealias GDExtensionInterfaceDictionaryOperatorIndexConstVar = CPointerVarOf<GDExtensionInterfaceDictionaryOperatorIndexConst>

typealias GDExtensionInterfaceDictionaryOperatorIndexConst = CPointer<CFunction<(GDExtensionConstTypePtr?, GDExtensionConstVariantPtr?) -> GDExtensionVariantPtr?>>

typealias GDExtensionInterfaceDictionarySetTypedVar = CPointerVarOf<GDExtensionInterfaceDictionarySetTyped>

typealias GDExtensionInterfaceDictionarySetTyped = CPointer<CFunction<(GDExtensionTypePtr?, GDExtensionVariantType, GDExtensionConstStringNamePtr?, GDExtensionConstVariantPtr?, GDExtensionVariantType, Unit>>

typealias GDExtensionInterfaceObjectMethodBindCallVar = CPointerVarOf<GDExtensionInterfaceObjectMethodBindCall>

typealias GDExtensionInterfaceObjectMethodBindCall = CPointer<CFunction<(GDExtensionMethodBindPtr?, GDExtensionObjectPtr?, CPointer<GDExtensionConstVariantPtrVar>?, GDExtensionInt, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionCallError>?) -> Unit>>

typealias GDExtensionInterfaceObjectMethodBindPtrcallVar = CPointerVarOf<GDExtensionInterfaceObjectMethodBindPtrcall>

typealias GDExtensionInterfaceObjectMethodBindPtrcall = CPointer<CFunction<(GDExtensionMethodBindPtr?, GDExtensionObjectPtr?, CPointer<GDExtensionConstTypePtrVar>?, GDExtensionTypePtr?) -> Unit>>

typealias GDExtensionInterfaceObjectDestroyVar = CPointerVarOf<GDExtensionInterfaceObjectDestroy>

typealias GDExtensionInterfaceObjectDestroy = CPointer<CFunction<(GDExtensionObjectPtr?) -> Unit>>

typealias GDExtensionInterfaceGlobalGetSingletonVar = CPointerVarOf<GDExtensionInterfaceGlobalGetSingleton>

typealias GDExtensionInterfaceGlobalGetSingleton = CPointer<CFunction<(GDExtensionObjectPtr?>>

typealias GDExtensionInterfaceObjectGetInstanceBindingVar = CPointerVarOf<GDExtensionInterfaceObjectGetInstanceBinding>

typealias GDExtensionInterfaceObjectGetInstanceBinding = CPointer<CFunction<(GDExtensionObjectPtr?, COpaquePointer?, CPointer<GDExtensionInstanceBindingCallbacks>?) -> COpaquePointer?>>

typealias GDExtensionInterfaceObjectSetInstanceBindingVar = CPointerVarOf<GDExtensionInterfaceObjectSetInstanceBinding>

typealias GDExtensionInterfaceObjectSetInstanceBinding = CPointer<CFunction<(GDExtensionObjectPtr?, COpaquePointer?, COpaquePointer?, CPointer<GDExtensionInstanceBindingCallbacks>?) -> Unit>>

typealias GDExtensionInterfaceObjectFreeInstanceBindingVar = CPointerVarOf<GDExtensionInterfaceObjectFreeInstanceBinding>

typealias GDExtensionInterfaceObjectFreeInstanceBinding = CPointer<CFunction<(GDExtensionObjectPtr?, COpaquePointer?) -> Unit>>

typealias GDExtensionInterfaceObjectSetInstanceVar = CPointerVarOf<GDExtensionInterfaceObjectSetInstance>

typealias GDExtensionInterfaceObjectSetInstance = CPointer<CFunction<(GDExtensionObjectPtr?, GDExtensionConstStringNamePtr?, GDExtensionClassInstancePtr?) -> Unit>>

typealias GDExtensionInterfaceObjectGetClassNameVar = CPointerVarOf<GDExtensionInterfaceObjectGetClassName>

typealias GDExtensionInterfaceObjectGetClassName = CPointer<CFunction<(GDExtensionConstObjectPtr?, GDExtensionClassLibraryPtr?, GDExtensionUninitializedStringNamePtr?) -> GDExtensionBool>>

typealias GDExtensionInterfaceObjectCastToVar = CPointerVarOf<GDExtensionInterfaceObjectCastTo>

typealias GDExtensionInterfaceObjectCastTo = CPointer<CFunction<(GDExtensionConstObjectPtr?, COpaquePointer?) -> GDExtensionObjectPtr?>>

typealias GDExtensionInterfaceObjectGetInstanceFromIdVar = CPointerVarOf<GDExtensionInterfaceObjectGetInstanceFromId>

typealias GDExtensionInterfaceObjectGetInstanceFromId = CPointer<CFunction<(GDObjectInstanceID) -> GDExtensionObjectPtr?>>

typealias GDExtensionInterfaceObjectGetInstanceIdVar = CPointerVarOf<GDExtensionInterfaceObjectGetInstanceId>

typealias GDExtensionInterfaceObjectGetInstanceId = CPointer<CFunction<(GDExtensionConstObjectPtr?) -> GDObjectInstanceID>>

typealias GDExtensionInterfaceObjectHasScriptMethodVar = CPointerVarOf<GDExtensionInterfaceObjectHasScriptMethod>

typealias GDExtensionInterfaceObjectHasScriptMethod = CPointer<CFunction<(GDExtensionConstObjectPtr?, GDExtensionBool>>

typealias GDExtensionInterfaceObjectCallScriptMethodVar = CPointerVarOf<GDExtensionInterfaceObjectCallScriptMethod>

typealias GDExtensionInterfaceObjectCallScriptMethod = CPointer<CFunction<(GDExtensionObjectPtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionConstVariantPtrVar>?, GDExtensionInt, GDExtensionUninitializedVariantPtr?, CPointer<GDExtensionCallError>?) -> Unit>>

typealias GDExtensionInterfaceRefGetObjectVar = CPointerVarOf<GDExtensionInterfaceRefGetObject>

typealias GDExtensionInterfaceRefGetObject = CPointer<CFunction<(GDExtensionConstRefPtr?) -> GDExtensionObjectPtr?>>

typealias GDExtensionInterfaceRefSetObjectVar = CPointerVarOf<GDExtensionInterfaceRefSetObject>

typealias GDExtensionInterfaceRefSetObject = CPointer<CFunction<(GDExtensionRefPtr?, GDExtensionObjectPtr?) -> Unit>>

typealias GDExtensionInterfaceScriptInstanceCreateVar = CPointerVarOf<GDExtensionInterfaceScriptInstanceCreate>

typealias GDExtensionInterfaceScriptInstanceCreate = CPointer<CFunction<(CPointer<GDExtensionScriptInstanceInfo>?, GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstancePtr?>>

typealias GDExtensionInterfaceScriptInstanceCreate2Var = CPointerVarOf<GDExtensionInterfaceScriptInstanceCreate2>

typealias GDExtensionInterfaceScriptInstanceCreate2 = CPointer<CFunction<(CPointer<GDExtensionScriptInstanceInfo2>?, GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstancePtr?>>

typealias GDExtensionInterfaceScriptInstanceCreate3Var = CPointerVarOf<GDExtensionInterfaceScriptInstanceCreate3>

typealias GDExtensionInterfaceScriptInstanceCreate3 = CPointer<CFunction<(CPointer<GDExtensionScriptInstanceInfo3>?, GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstancePtr?>>

typealias GDExtensionInterfacePlaceHolderScriptInstanceCreateVar = CPointerVarOf<GDExtensionInterfacePlaceHolderScriptInstanceCreate>

typealias GDExtensionInterfacePlaceHolderScriptInstanceCreate = CPointer<CFunction<(GDExtensionObjectPtr?, GDExtensionObjectPtr?, GDExtensionObjectPtr?) -> GDExtensionScriptInstancePtr?>>

typealias GDExtensionInterfacePlaceHolderScriptInstanceUpdateVar = CPointerVarOf<GDExtensionInterfacePlaceHolderScriptInstanceUpdate>

typealias GDExtensionInterfacePlaceHolderScriptInstanceUpdate = CPointer<CFunction<(GDExtensionScriptInstancePtr?, GDExtensionConstTypePtr?, GDExtensionConstTypePtr?) -> Unit>>

typealias GDExtensionInterfaceObjectGetScriptInstanceVar = CPointerVarOf<GDExtensionInterfaceObjectGetScriptInstance>

typealias GDExtensionInterfaceObjectGetScriptInstance = CPointer<CFunction<(GDExtensionConstObjectPtr?, GDExtensionObjectPtr?) -> GDExtensionScriptInstanceDataPtr?>>

typealias GDExtensionInterfaceObjectSetScriptInstanceVar = CPointerVarOf<GDExtensionInterfaceObjectSetScriptInstance>

typealias GDExtensionInterfaceObjectSetScriptInstance = CPointer<CFunction<(GDExtensionObjectPtr?, GDExtensionScriptInstanceDataPtr?) -> Unit>>

typealias GDExtensionInterfaceCallableCustomCreateVar = CPointerVarOf<GDExtensionInterfaceCallableCustomCreate>

typealias GDExtensionInterfaceCallableCustomCreate = CPointer<CFunction<(GDExtensionUninitializedTypePtr?, CPointer<GDExtensionCallableCustomInfo>?) -> Unit>>

typealias GDExtensionInterfaceCallableCustomCreate2Var = CPointerVarOf<GDExtensionInterfaceCallableCustomCreate2>

typealias GDExtensionInterfaceCallableCustomCreate2 = CPointer<CFunction<(GDExtensionUninitializedTypePtr?, CPointer<GDExtensionCallableCustomInfo2>?) -> Unit>>

typealias GDExtensionInterfaceCallableCustomGetUserDataVar = CPointerVarOf<GDExtensionInterfaceCallableCustomGetUserData>

typealias GDExtensionInterfaceCallableCustomGetUserData = CPointer<CFunction<(GDExtensionConstTypePtr?, COpaquePointer?) -> COpaquePointer?>>

typealias GDExtensionInterfaceClassdbConstructObjectVar = CPointerVarOf<GDExtensionInterfaceClassdbConstructObject>

typealias GDExtensionInterfaceClassdbConstructObject = CPointer<CFunction<(GDExtensionObjectPtr?>>

typealias GDExtensionInterfaceClassdbConstructObject2Var = CPointerVarOf<GDExtensionInterfaceClassdbConstructObject2>

typealias GDExtensionInterfaceClassdbConstructObject2 = CPointer<CFunction<(GDExtensionObjectPtr?>>

typealias GDExtensionInterfaceClassdbGetMethodBindVar = CPointerVarOf<GDExtensionInterfaceClassdbGetMethodBind>

typealias GDExtensionInterfaceClassdbGetMethodBind = CPointer<CFunction<(GDExtensionConstStringNamePtr?, GDExtensionConstStringNamePtr?, GDExtensionInt) -> GDExtensionMethodBindPtr?>>

typealias GDExtensionInterfaceClassdbGetClassTagVar = CPointerVarOf<GDExtensionInterfaceClassdbGetClassTag>

typealias GDExtensionInterfaceClassdbGetClassTag = CPointer<CFunction<(COpaquePointer?>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassVar = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClass>

typealias GDExtensionInterfaceClassdbRegisterExtensionClass = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionClassCreationInfo>?) -> Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClass2Var = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClass2>

typealias GDExtensionInterfaceClassdbRegisterExtensionClass2 = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionClassCreationInfo2>?) -> Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClass3Var = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClass3>

typealias GDExtensionInterfaceClassdbRegisterExtensionClass3 = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionClassCreationInfo3>?) -> Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClass4Var = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClass4>

typealias GDExtensionInterfaceClassdbRegisterExtensionClass4 = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionClassCreationInfo4>?) -> Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClass5Var = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClass5>

typealias GDExtensionInterfaceClassdbRegisterExtensionClass5 = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionClassCreationInfo5>?) -> Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassMethodVar = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClassMethod>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassMethod = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionClassMethodInfo>?) -> Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassVirtualMethodVar = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClassVirtualMethod>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassVirtualMethod = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionClassVirtualMethodInfo>?) -> Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassIntegerConstantVar = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClassIntegerConstant>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassIntegerConstant = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, GDExtensionConstStringNamePtr?, GDExtensionConstStringNamePtr?, GDExtensionInt, GDExtensionBool) -> Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassPropertyVar = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClassProperty>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassProperty = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionPropertyInfo>?, GDExtensionConstStringNamePtr?, Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassPropertyIndexedVar = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClassPropertyIndexed>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassPropertyIndexed = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionPropertyInfo>?, GDExtensionConstStringNamePtr?, GDExtensionConstStringNamePtr?, GDExtensionInt) -> Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassPropertyGroupVar = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClassPropertyGroup>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassPropertyGroup = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, GDExtensionConstStringPtr?, GDExtensionConstStringPtr?) -> Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassPropertySubgroupVar = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClassPropertySubgroup>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassPropertySubgroup = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, GDExtensionConstStringPtr?, GDExtensionConstStringPtr?) -> Unit>>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassSignalVar = CPointerVarOf<GDExtensionInterfaceClassdbRegisterExtensionClassSignal>

typealias GDExtensionInterfaceClassdbRegisterExtensionClassSignal = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionConstStringNamePtr?, GDExtensionConstStringNamePtr?, CPointer<GDExtensionPropertyInfo>?, GDExtensionInt) -> Unit>>

typealias GDExtensionInterfaceClassdbUnregisterExtensionClassVar = CPointerVarOf<GDExtensionInterfaceClassdbUnregisterExtensionClass>

typealias GDExtensionInterfaceClassdbUnregisterExtensionClass = CPointer<CFunction<(GDExtensionClassLibraryPtr?, Unit>>

typealias GDExtensionInterfaceGetLibraryPathVar = CPointerVarOf<GDExtensionInterfaceGetLibraryPath>

typealias GDExtensionInterfaceGetLibraryPath = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionUninitializedStringPtr?) -> Unit>>

typealias GDExtensionInterfaceEditorAddPluginVar = CPointerVarOf<GDExtensionInterfaceEditorAddPlugin>

typealias GDExtensionInterfaceEditorAddPlugin = CPointer<CFunction<(Unit>>

typealias GDExtensionInterfaceEditorRemovePluginVar = CPointerVarOf<GDExtensionInterfaceEditorRemovePlugin>

typealias GDExtensionInterfaceEditorRemovePlugin = CPointer<CFunction<(Unit>>

typealias GDExtensionsInterfaceEditorHelpLoadXmlFromUtf8CharsVar = CPointerVarOf<GDExtensionsInterfaceEditorHelpLoadXmlFromUtf8Chars>

typealias GDExtensionsInterfaceEditorHelpLoadXmlFromUtf8Chars = CPointer<CFunction<(CPointer<ByteVar>?) -> Unit>>

typealias GDExtensionsInterfaceEditorHelpLoadXmlFromUtf8CharsAndLenVar = CPointerVarOf<GDExtensionsInterfaceEditorHelpLoadXmlFromUtf8CharsAndLen>

typealias GDExtensionsInterfaceEditorHelpLoadXmlFromUtf8CharsAndLen = CPointer<CFunction<(CPointer<ByteVar>?, GDExtensionInt) -> Unit>>

typealias GDExtensionInterfaceEditorRegisterGetClassesUsedCallbackVar = CPointerVarOf<GDExtensionInterfaceEditorRegisterGetClassesUsedCallback>

typealias GDExtensionInterfaceEditorRegisterGetClassesUsedCallback = CPointer<CFunction<(GDExtensionClassLibraryPtr?, GDExtensionEditorGetClassesUsedCallback?) -> Unit>>

typealias GDExtensionInterfaceRegisterMainLoopCallbacksVar = CPointerVarOf<GDExtensionInterfaceRegisterMainLoopCallbacks>

typealias GDExtensionInterfaceRegisterMainLoopCallbacks = CPointer<CFunction<(GDExtensionClassLibraryPtr?, CPointer<GDExtensionMainLoopCallbacks>?) -> Unit>>

class GDExtensionCallError constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var error: GDExtensionCallErrorType
        external get() {  }
        external set(value: GDExtensionCallErrorType) {  }

    var argument: int32_t
        external get() {  }
        external set(value: int32_t) {  }

    var expected: int32_t
        external get() {  }
        external set(value: int32_t) {  }
}

class GDExtensionInstanceBindingCallbacks constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var create_callback: GDExtensionInstanceBindingCreateCallback?
        external get() {  }
        external set(value: GDExtensionInstanceBindingCreateCallback?) {  }

    var free_callback: GDExtensionInstanceBindingFreeCallback?
        external get() {  }
        external set(value: GDExtensionInstanceBindingFreeCallback?) {  }

    var reference_callback: GDExtensionInstanceBindingReferenceCallback?
        external get() {  }
        external set(value: GDExtensionInstanceBindingReferenceCallback?) {  }
}

class GDExtensionPropertyInfo constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var type: GDExtensionVariantType
        external get() {  }
        external set(value: GDExtensionVariantType) {  }

    var name: GDExtensionStringNamePtr?
        external get() {  }
        external set(value: GDExtensionStringNamePtr?) {  }

    var class_name: GDExtensionStringNamePtr?
        external get() {  }
        external set(value: GDExtensionStringNamePtr?) {  }

    var hint: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var hint_string: GDExtensionStringPtr?
        external get() {  }
        external set(value: GDExtensionStringPtr?) {  }

    var usage: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }
}

class GDExtensionMethodInfo constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var name: GDExtensionStringNamePtr?
        external get() {  }
        external set(value: GDExtensionStringNamePtr?) {  }

    val return_value: GDExtensionPropertyInfo
        external get() {  }

    var flags: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var id: int32_t
        external get() {  }
        external set(value: int32_t) {  }

    var argument_count: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var arguments: CPointer<GDExtensionPropertyInfo>?
        external get() {  }
        external set(value: CPointer<GDExtensionPropertyInfo>?) {  }

    var default_argument_count: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var default_arguments: CPointer<CPointerVarOf<GDExtensionVariantPtrGDExtensionVariantPtrVar>?
        external get() {  }
        external set(value: CPointer<CPointerVarOf<GDExtensionVariantPtrGDExtensionVariantPtrVar>?) {  }
}

class GDExtensionClassCreationInfo constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var is_virtual: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var is_abstract: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var set_func: GDExtensionClassSet?
        external get() {  }
        external set(value: GDExtensionClassSet?) {  }

    var get_func: GDExtensionClassGet?
        external get() {  }
        external set(value: GDExtensionClassGet?) {  }

    var get_property_list_func: GDExtensionClassGetPropertyList?
        external get() {  }
        external set(value: GDExtensionClassGetPropertyList?) {  }

    var free_property_list_func: GDExtensionClassFreePropertyList?
        external get() {  }
        external set(value: GDExtensionClassFreePropertyList?) {  }

    var property_can_revert_func: GDExtensionClassPropertyCanRevert?
        external get() {  }
        external set(value: GDExtensionClassPropertyCanRevert?) {  }

    var property_get_revert_func: GDExtensionClassPropertyGetRevert?
        external get() {  }
        external set(value: GDExtensionClassPropertyGetRevert?) {  }

    var notification_func: GDExtensionClassNotification?
        external get() {  }
        external set(value: GDExtensionClassNotification?) {  }

    var to_string_func: GDExtensionClassToString?
        external get() {  }
        external set(value: GDExtensionClassToString?) {  }

    var reference_func: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassReference?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassReference?) {  }

    var unreference_func: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassUnreference?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassUnreference?) {  }

    var create_instance_func: CPointer<CFunction<(COpaquePointer?) -> GDExtensionObjectPtr?GDExtensionClassCreateInstance?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?) -> GDExtensionObjectPtr?GDExtensionClassCreateInstance?) {  }

    var free_instance_func: CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?) -> GDExtensionClassFreeInstance?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?) -> GDExtensionClassFreeInstance?) {  }

    var get_virtual_func: GDExtensionClassCallVirtual?GDExtensionClassGetVirtual?
        external get() {  }
        external set(value: GDExtensionClassCallVirtual?GDExtensionClassGetVirtual?) {  }

    var get_rid_func: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> uint64_tGDExtensionClassGetRID?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> uint64_tGDExtensionClassGetRID?) {  }

    var class_userdata: COpaquePointer?
        external get() {  }
        external set(value: COpaquePointer?) {  }
}

class GDExtensionClassCreationInfo2 constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var is_virtual: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var is_abstract: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var is_exposed: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var set_func: GDExtensionClassSet?
        external get() {  }
        external set(value: GDExtensionClassSet?) {  }

    var get_func: GDExtensionClassGet?
        external get() {  }
        external set(value: GDExtensionClassGet?) {  }

    var get_property_list_func: GDExtensionClassGetPropertyList?
        external get() {  }
        external set(value: GDExtensionClassGetPropertyList?) {  }

    var free_property_list_func: GDExtensionClassFreePropertyList?
        external get() {  }
        external set(value: GDExtensionClassFreePropertyList?) {  }

    var property_can_revert_func: GDExtensionClassPropertyCanRevert?
        external get() {  }
        external set(value: GDExtensionClassPropertyCanRevert?) {  }

    var property_get_revert_func: GDExtensionClassPropertyGetRevert?
        external get() {  }
        external set(value: GDExtensionClassPropertyGetRevert?) {  }

    var validate_property_func: GDExtensionClassValidateProperty?
        external get() {  }
        external set(value: GDExtensionClassValidateProperty?) {  }

    var notification_func: CPointer<CFunction<(GDExtensionClassInstancePtr?, int32_t, GDExtensionBool) -> GDExtensionClassNotification2?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?, int32_t, GDExtensionBool) -> GDExtensionClassNotification2?) {  }

    var to_string_func: GDExtensionClassToString?
        external get() {  }
        external set(value: GDExtensionClassToString?) {  }

    var reference_func: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassReference?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassReference?) {  }

    var unreference_func: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassUnreference?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassUnreference?) {  }

    var create_instance_func: CPointer<CFunction<(COpaquePointer?) -> GDExtensionObjectPtr?GDExtensionClassCreateInstance?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?) -> GDExtensionObjectPtr?GDExtensionClassCreateInstance?) {  }

    var free_instance_func: CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?) -> GDExtensionClassFreeInstance?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?) -> GDExtensionClassFreeInstance?) {  }

    var recreate_instance_func: CPointer<CFunction<(COpaquePointer?, GDExtensionObjectPtr?) -> GDExtensionClassRecreateInstance?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionObjectPtr?) -> GDExtensionClassRecreateInstance?) {  }

    var get_virtual_func: GDExtensionClassCallVirtual?GDExtensionClassGetVirtual?
        external get() {  }
        external set(value: GDExtensionClassCallVirtual?GDExtensionClassGetVirtual?) {  }

    var get_virtual_call_data_func: CPointer<CFunction<(COpaquePointer?, COpaquePointer?GDExtensionClassGetVirtualCallData?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, COpaquePointer?GDExtensionClassGetVirtualCallData?) {  }

    var call_virtual_with_data_func: GDExtensionClassCallVirtualWithData?
        external get() {  }
        external set(value: GDExtensionClassCallVirtualWithData?) {  }

    var get_rid_func: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> uint64_tGDExtensionClassGetRID?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> uint64_tGDExtensionClassGetRID?) {  }

    var class_userdata: COpaquePointer?
        external get() {  }
        external set(value: COpaquePointer?) {  }
}

class GDExtensionClassCreationInfo3 constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var is_virtual: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var is_abstract: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var is_exposed: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var is_runtime: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var set_func: GDExtensionClassSet?
        external get() {  }
        external set(value: GDExtensionClassSet?) {  }

    var get_func: GDExtensionClassGet?
        external get() {  }
        external set(value: GDExtensionClassGet?) {  }

    var get_property_list_func: GDExtensionClassGetPropertyList?
        external get() {  }
        external set(value: GDExtensionClassGetPropertyList?) {  }

    var free_property_list_func: GDExtensionClassFreePropertyList2?
        external get() {  }
        external set(value: GDExtensionClassFreePropertyList2?) {  }

    var property_can_revert_func: GDExtensionClassPropertyCanRevert?
        external get() {  }
        external set(value: GDExtensionClassPropertyCanRevert?) {  }

    var property_get_revert_func: GDExtensionClassPropertyGetRevert?
        external get() {  }
        external set(value: GDExtensionClassPropertyGetRevert?) {  }

    var validate_property_func: GDExtensionClassValidateProperty?
        external get() {  }
        external set(value: GDExtensionClassValidateProperty?) {  }

    var notification_func: CPointer<CFunction<(GDExtensionClassInstancePtr?, int32_t, GDExtensionBool) -> GDExtensionClassNotification2?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?, int32_t, GDExtensionBool) -> GDExtensionClassNotification2?) {  }

    var to_string_func: GDExtensionClassToString?
        external get() {  }
        external set(value: GDExtensionClassToString?) {  }

    var reference_func: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassReference?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassReference?) {  }

    var unreference_func: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassUnreference?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassUnreference?) {  }

    var create_instance_func: CPointer<CFunction<(COpaquePointer?) -> GDExtensionObjectPtr?GDExtensionClassCreateInstance?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?) -> GDExtensionObjectPtr?GDExtensionClassCreateInstance?) {  }

    var free_instance_func: CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?) -> GDExtensionClassFreeInstance?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?) -> GDExtensionClassFreeInstance?) {  }

    var recreate_instance_func: CPointer<CFunction<(COpaquePointer?, GDExtensionObjectPtr?) -> GDExtensionClassRecreateInstance?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionObjectPtr?) -> GDExtensionClassRecreateInstance?) {  }

    var get_virtual_func: GDExtensionClassCallVirtual?GDExtensionClassGetVirtual?
        external get() {  }
        external set(value: GDExtensionClassCallVirtual?GDExtensionClassGetVirtual?) {  }

    var get_virtual_call_data_func: CPointer<CFunction<(COpaquePointer?, COpaquePointer?GDExtensionClassGetVirtualCallData?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, COpaquePointer?GDExtensionClassGetVirtualCallData?) {  }

    var call_virtual_with_data_func: GDExtensionClassCallVirtualWithData?
        external get() {  }
        external set(value: GDExtensionClassCallVirtualWithData?) {  }

    var get_rid_func: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> uint64_tGDExtensionClassGetRID?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> uint64_tGDExtensionClassGetRID?) {  }

    var class_userdata: COpaquePointer?
        external get() {  }
        external set(value: COpaquePointer?) {  }
}

class GDExtensionClassCreationInfo4 constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var is_virtual: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var is_abstract: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var is_exposed: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var is_runtime: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var icon_path: GDExtensionConstStringPtr?
        external get() {  }
        external set(value: GDExtensionConstStringPtr?) {  }

    var set_func: GDExtensionClassSet?
        external get() {  }
        external set(value: GDExtensionClassSet?) {  }

    var get_func: GDExtensionClassGet?
        external get() {  }
        external set(value: GDExtensionClassGet?) {  }

    var get_property_list_func: GDExtensionClassGetPropertyList?
        external get() {  }
        external set(value: GDExtensionClassGetPropertyList?) {  }

    var free_property_list_func: GDExtensionClassFreePropertyList2?
        external get() {  }
        external set(value: GDExtensionClassFreePropertyList2?) {  }

    var property_can_revert_func: GDExtensionClassPropertyCanRevert?
        external get() {  }
        external set(value: GDExtensionClassPropertyCanRevert?) {  }

    var property_get_revert_func: GDExtensionClassPropertyGetRevert?
        external get() {  }
        external set(value: GDExtensionClassPropertyGetRevert?) {  }

    var validate_property_func: GDExtensionClassValidateProperty?
        external get() {  }
        external set(value: GDExtensionClassValidateProperty?) {  }

    var notification_func: CPointer<CFunction<(GDExtensionClassInstancePtr?, int32_t, GDExtensionBool) -> GDExtensionClassNotification2?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?, int32_t, GDExtensionBool) -> GDExtensionClassNotification2?) {  }

    var to_string_func: GDExtensionClassToString?
        external get() {  }
        external set(value: GDExtensionClassToString?) {  }

    var reference_func: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassReference?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassReference?) {  }

    var unreference_func: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassUnreference?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionClassInstancePtr?) -> GDExtensionClassUnreference?) {  }

    var create_instance_func: CPointer<CFunction<(COpaquePointer?, GDExtensionBool) -> GDExtensionObjectPtr?GDExtensionClassCreateInstance2?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionBool) -> GDExtensionObjectPtr?GDExtensionClassCreateInstance2?) {  }

    var free_instance_func: CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?) -> GDExtensionClassFreeInstance?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionClassInstancePtr?) -> GDExtensionClassFreeInstance?) {  }

    var recreate_instance_func: CPointer<CFunction<(COpaquePointer?, GDExtensionObjectPtr?) -> GDExtensionClassRecreateInstance?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionObjectPtr?) -> GDExtensionClassRecreateInstance?) {  }

    var get_virtual_func: GDExtensionClassCallVirtual?GDExtensionClassGetVirtual2?
        external get() {  }
        external set(value: GDExtensionClassCallVirtual?GDExtensionClassGetVirtual2?) {  }

    var get_virtual_call_data_func: GDExtensionClassGetVirtualCallData2?
        external get() {  }
        external set(value: GDExtensionClassGetVirtualCallData2?) {  }

    var call_virtual_with_data_func: GDExtensionClassCallVirtualWithData?
        external get() {  }
        external set(value: GDExtensionClassCallVirtualWithData?) {  }

    var class_userdata: COpaquePointer?
        external get() {  }
        external set(value: COpaquePointer?) {  }
}

class GDExtensionClassMethodInfo constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var name: GDExtensionStringNamePtr?
        external get() {  }
        external set(value: GDExtensionStringNamePtr?) {  }

    var method_userdata: COpaquePointer?
        external get() {  }
        external set(value: COpaquePointer?) {  }

    var call_func: GDExtensionClassMethodCall?
        external get() {  }
        external set(value: GDExtensionClassMethodCall?) {  }

    var ptrcall_func: GDExtensionClassMethodPtrCall?
        external get() {  }
        external set(value: GDExtensionClassMethodPtrCall?) {  }

    var method_flags: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var has_return_value: GDExtensionBool
        external get() {  }
        external set(value: GDExtensionBool) {  }

    var return_value_info: CPointer<GDExtensionPropertyInfo>?
        external get() {  }
        external set(value: CPointer<GDExtensionPropertyInfo>?) {  }

    var return_value_metadata: GDExtensionClassMethodArgumentMetadata
        external get() {  }
        external set(value: GDExtensionClassMethodArgumentMetadata) {  }

    var argument_count: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var arguments_info: CPointer<GDExtensionPropertyInfo>?
        external get() {  }
        external set(value: CPointer<GDExtensionPropertyInfo>?) {  }

    var arguments_metadata: CPointer<GDExtensionClassMethodArgumentMetadata.Var>?
        external get() {  }
        external set(value: CPointer<GDExtensionClassMethodArgumentMetadata.Var>?) {  }

    var default_argument_count: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var default_arguments: CPointer<CPointerVarOf<GDExtensionVariantPtrGDExtensionVariantPtrVar>?
        external get() {  }
        external set(value: CPointer<CPointerVarOf<GDExtensionVariantPtrGDExtensionVariantPtrVar>?) {  }
}

class GDExtensionClassVirtualMethodInfo constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var name: GDExtensionStringNamePtr?
        external get() {  }
        external set(value: GDExtensionStringNamePtr?) {  }

    var method_flags: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    val return_value: GDExtensionPropertyInfo
        external get() {  }

    var return_value_metadata: GDExtensionClassMethodArgumentMetadata
        external get() {  }
        external set(value: GDExtensionClassMethodArgumentMetadata) {  }

    var argument_count: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var arguments: CPointer<GDExtensionPropertyInfo>?
        external get() {  }
        external set(value: CPointer<GDExtensionPropertyInfo>?) {  }

    var arguments_metadata: CPointer<GDExtensionClassMethodArgumentMetadata.Var>?
        external get() {  }
        external set(value: CPointer<GDExtensionClassMethodArgumentMetadata.Var>?) {  }
}

class GDExtensionCallableCustomInfo constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var callable_userdata: COpaquePointer?
        external get() {  }
        external set(value: COpaquePointer?) {  }

    var token: COpaquePointer?
        external get() {  }
        external set(value: COpaquePointer?) {  }

    var object_id: GDObjectInstanceID
        external get() {  }
        external set(value: GDObjectInstanceID) {  }

    var call_func: GDExtensionCallableCustomCall?
        external get() {  }
        external set(value: GDExtensionCallableCustomCall?) {  }

    var is_valid_func: CPointer<CFunction<(COpaquePointer?) -> GDExtensionCallableCustomIsValid?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?) -> GDExtensionCallableCustomIsValid?) {  }

    var free_func: CPointer<CFunction<(COpaquePointer?) -> GDExtensionCallableCustomFree?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?) -> GDExtensionCallableCustomFree?) {  }

    var hash_func: CPointer<CFunction<(COpaquePointer?) -> uint32_tGDExtensionCallableCustomHash?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?) -> uint32_tGDExtensionCallableCustomHash?) {  }

    var equal_func: CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> GDExtensionCallableCustomEqual?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> GDExtensionCallableCustomEqual?) {  }

    var less_than_func: CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> GDExtensionCallableCustomLessThan?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> GDExtensionCallableCustomLessThan?) {  }

    var to_string_func: CPointer<CFunction<(COpaquePointer?, GDExtensionBoolVar>?, GDExtensionStringPtr?) -> GDExtensionCallableCustomToString?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionBoolVar>?, GDExtensionStringPtr?) -> GDExtensionCallableCustomToString?) {  }
}

class GDExtensionCallableCustomInfo2 constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var callable_userdata: COpaquePointer?
        external get() {  }
        external set(value: COpaquePointer?) {  }

    var token: COpaquePointer?
        external get() {  }
        external set(value: COpaquePointer?) {  }

    var object_id: GDObjectInstanceID
        external get() {  }
        external set(value: GDObjectInstanceID) {  }

    var call_func: GDExtensionCallableCustomCall?
        external get() {  }
        external set(value: GDExtensionCallableCustomCall?) {  }

    var is_valid_func: CPointer<CFunction<(COpaquePointer?) -> GDExtensionCallableCustomIsValid?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?) -> GDExtensionCallableCustomIsValid?) {  }

    var free_func: CPointer<CFunction<(COpaquePointer?) -> GDExtensionCallableCustomFree?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?) -> GDExtensionCallableCustomFree?) {  }

    var hash_func: CPointer<CFunction<(COpaquePointer?) -> uint32_tGDExtensionCallableCustomHash?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?) -> uint32_tGDExtensionCallableCustomHash?) {  }

    var equal_func: CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> GDExtensionCallableCustomEqual?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> GDExtensionCallableCustomEqual?) {  }

    var less_than_func: CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> GDExtensionCallableCustomLessThan?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> GDExtensionCallableCustomLessThan?) {  }

    var to_string_func: CPointer<CFunction<(COpaquePointer?, GDExtensionBoolVar>?, GDExtensionStringPtr?) -> GDExtensionCallableCustomToString?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionBoolVar>?, GDExtensionStringPtr?) -> GDExtensionCallableCustomToString?) {  }

    var get_argument_count_func: CPointer<CFunction<(COpaquePointer?, GDExtensionBoolVar>?) -> GDExtensionIntGDExtensionCallableCustomGetArgumentCount?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionBoolVar>?) -> GDExtensionIntGDExtensionCallableCustomGetArgumentCount?) {  }
}

class GDExtensionScriptInstanceInfo constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var set_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?) {  }

    var get_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?) {  }

    var get_property_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetPropertyList?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetPropertyList?) {  }

    var free_property_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceFreePropertyList?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceFreePropertyList?) {  }

    var property_can_revert_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyCanRevert?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyCanRevert?) {  }

    var property_get_revert_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyGetRevert?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyGetRevert?) {  }

    var get_owner_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetOwner?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetOwner?) {  }

    var get_property_state_func: GDExtensionScriptInstanceGetPropertyState?
        external get() {  }
        external set(value: GDExtensionScriptInstanceGetPropertyState?) {  }

    var get_method_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetMethodList?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetMethodList?) {  }

    var free_method_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionMethodInfo>?) -> GDExtensionScriptInstanceFreeMethodList?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionMethodInfo>?) -> GDExtensionScriptInstanceFreeMethodList?) {  }

    var get_property_type_func: GDExtensionScriptInstanceGetPropertyType?
        external get() {  }
        external set(value: GDExtensionScriptInstanceGetPropertyType?) {  }

    var has_method_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceHasMethod?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceHasMethod?) {  }

    var call_func: GDExtensionScriptInstanceCall?
        external get() {  }
        external set(value: GDExtensionScriptInstanceCall?) {  }

    var notification_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, int32_t) -> GDExtensionScriptInstanceNotification?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, int32_t) -> GDExtensionScriptInstanceNotification?) {  }

    var to_string_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionBoolVar>?, GDExtensionStringPtr?) -> GDExtensionScriptInstanceToString?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionBoolVar>?, GDExtensionStringPtr?) -> GDExtensionScriptInstanceToString?) {  }

    var refcount_incremented_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountIncremented?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountIncremented?) {  }

    var refcount_decremented_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountDecremented?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountDecremented?) {  }

    var get_script_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetScript?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetScript?) {  }

    var is_placeholder_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceIsPlaceholder?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceIsPlaceholder?) {  }

    var set_fallback_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?) {  }

    var get_fallback_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?) {  }

    var get_language_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptLanguagePtr?GDExtensionScriptInstanceGetLanguage?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptLanguagePtr?GDExtensionScriptInstanceGetLanguage?) {  }

    var free_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceFree?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceFree?) {  }
}

class GDExtensionScriptInstanceInfo2 constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var set_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?) {  }

    var get_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?) {  }

    var get_property_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetPropertyList?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetPropertyList?) {  }

    var free_property_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceFreePropertyList?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceFreePropertyList?) {  }

    var get_class_category_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceGetClassCategory?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceGetClassCategory?) {  }

    var property_can_revert_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyCanRevert?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyCanRevert?) {  }

    var property_get_revert_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyGetRevert?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyGetRevert?) {  }

    var get_owner_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetOwner?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetOwner?) {  }

    var get_property_state_func: GDExtensionScriptInstanceGetPropertyState?
        external get() {  }
        external set(value: GDExtensionScriptInstanceGetPropertyState?) {  }

    var get_method_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetMethodList?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetMethodList?) {  }

    var free_method_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionMethodInfo>?) -> GDExtensionScriptInstanceFreeMethodList?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionMethodInfo>?) -> GDExtensionScriptInstanceFreeMethodList?) {  }

    var get_property_type_func: GDExtensionScriptInstanceGetPropertyType?
        external get() {  }
        external set(value: GDExtensionScriptInstanceGetPropertyType?) {  }

    var validate_property_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceValidateProperty?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceValidateProperty?) {  }

    var has_method_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceHasMethod?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceHasMethod?) {  }

    var call_func: GDExtensionScriptInstanceCall?
        external get() {  }
        external set(value: GDExtensionScriptInstanceCall?) {  }

    var notification_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, int32_t, GDExtensionBool) -> GDExtensionScriptInstanceNotification2?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, int32_t, GDExtensionBool) -> GDExtensionScriptInstanceNotification2?) {  }

    var to_string_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionBoolVar>?, GDExtensionStringPtr?) -> GDExtensionScriptInstanceToString?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionBoolVar>?, GDExtensionStringPtr?) -> GDExtensionScriptInstanceToString?) {  }

    var refcount_incremented_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountIncremented?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountIncremented?) {  }

    var refcount_decremented_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountDecremented?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountDecremented?) {  }

    var get_script_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetScript?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetScript?) {  }

    var is_placeholder_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceIsPlaceholder?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceIsPlaceholder?) {  }

    var set_fallback_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?) {  }

    var get_fallback_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?) {  }

    var get_language_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptLanguagePtr?GDExtensionScriptInstanceGetLanguage?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptLanguagePtr?GDExtensionScriptInstanceGetLanguage?) {  }

    var free_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceFree?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceFree?) {  }
}

class GDExtensionScriptInstanceInfo3 constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var set_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?) {  }

    var get_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?) {  }

    var get_property_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetPropertyList?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetPropertyList?) {  }

    var free_property_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?, uint32_t) -> GDExtensionScriptInstanceFreePropertyList2?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?, uint32_t) -> GDExtensionScriptInstanceFreePropertyList2?) {  }

    var get_class_category_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceGetClassCategory?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceGetClassCategory?) {  }

    var property_can_revert_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyCanRevert?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyCanRevert?) {  }

    var property_get_revert_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyGetRevert?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstancePropertyGetRevert?) {  }

    var get_owner_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetOwner?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetOwner?) {  }

    var get_property_state_func: GDExtensionScriptInstanceGetPropertyState?
        external get() {  }
        external set(value: GDExtensionScriptInstanceGetPropertyState?) {  }

    var get_method_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetMethodList?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, uint32_tVar>?) -> GDExtensionScriptInstanceGetMethodList?) {  }

    var free_method_list_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionMethodInfo>?, uint32_t) -> GDExtensionScriptInstanceFreeMethodList2?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionMethodInfo>?, uint32_t) -> GDExtensionScriptInstanceFreeMethodList2?) {  }

    var get_property_type_func: GDExtensionScriptInstanceGetPropertyType?
        external get() {  }
        external set(value: GDExtensionScriptInstanceGetPropertyType?) {  }

    var validate_property_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceValidateProperty?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, CPointer<GDExtensionPropertyInfo>?) -> GDExtensionScriptInstanceValidateProperty?) {  }

    var has_method_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceHasMethod?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceHasMethod?) {  }

    var get_method_argument_count_func: GDExtensionIntGDExtensionScriptInstanceGetMethodArgumentCount?
        external get() {  }
        external set(value: GDExtensionIntGDExtensionScriptInstanceGetMethodArgumentCount?) {  }

    var call_func: GDExtensionScriptInstanceCall?
        external get() {  }
        external set(value: GDExtensionScriptInstanceCall?) {  }

    var notification_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, int32_t, GDExtensionBool) -> GDExtensionScriptInstanceNotification2?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, int32_t, GDExtensionBool) -> GDExtensionScriptInstanceNotification2?) {  }

    var to_string_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionBoolVar>?, GDExtensionStringPtr?) -> GDExtensionScriptInstanceToString?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionBoolVar>?, GDExtensionStringPtr?) -> GDExtensionScriptInstanceToString?) {  }

    var refcount_incremented_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountIncremented?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountIncremented?) {  }

    var refcount_decremented_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountDecremented?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceRefCountDecremented?) {  }

    var get_script_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetScript?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionObjectPtr?GDExtensionScriptInstanceGetScript?) {  }

    var is_placeholder_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceIsPlaceholder?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceIsPlaceholder?) {  }

    var set_fallback_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceSet?) {  }

    var get_fallback_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?, GDExtensionScriptInstanceGet?) {  }

    var get_language_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptLanguagePtr?GDExtensionScriptInstanceGetLanguage?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptLanguagePtr?GDExtensionScriptInstanceGetLanguage?) {  }

    var free_func: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceFree?
        external get() {  }
        external set(value: CPointer<CFunction<(GDExtensionScriptInstanceDataPtr?) -> GDExtensionScriptInstanceFree?) {  }
}

class GDExtensionInitialization constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var minimum_initialization_level: GDExtensionInitializationLevel
        external get() {  }
        external set(value: GDExtensionInitializationLevel) {  }

    var userdata: COpaquePointer?
        external get() {  }
        external set(value: COpaquePointer?) {  }

    var initialize: CPointer<CFunction<(COpaquePointer?, GDExtensionInitializationLevel) -> GDExtensionInitializeCallback?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionInitializationLevel) -> GDExtensionInitializeCallback?) {  }

    var deinitialize: CPointer<CFunction<(COpaquePointer?, GDExtensionInitializationLevel) -> GDExtensionDeinitializeCallback?
        external get() {  }
        external set(value: CPointer<CFunction<(COpaquePointer?, GDExtensionInitializationLevel) -> GDExtensionDeinitializeCallback?) {  }
}

class GDExtensionGodotVersion constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var major: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var minor: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var patch: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var string: ByteVar>?
        external get() {  }
        external set(value: ByteVar>?) {  }
}

class GDExtensionGodotVersion2 constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var major: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var minor: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var patch: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var hex: uint32_t
        external get() {  }
        external set(value: uint32_t) {  }

    var status: ByteVar>?
        external get() {  }
        external set(value: ByteVar>?) {  }

    var build: ByteVar>?
        external get() {  }
        external set(value: ByteVar>?) {  }

    var hash: ByteVar>?
        external get() {  }
        external set(value: ByteVar>?) {  }

    var timestamp: uint64_t
        external get() {  }
        external set(value: uint64_t) {  }

    var string: ByteVar>?
        external get() {  }
        external set(value: ByteVar>?) {  }
}

class GDExtensionMainLoopCallbacks constructor(rawPtr: NativePtr) : CStructVar {
    @Deprecated companion object : CStructVar.Type

    var startup_func: GDExtensionMainLoopStartupCallback?
        external get() {  }
        external set(value: GDExtensionMainLoopStartupCallback?) {  }

    var shutdown_func: GDExtensionMainLoopShutdownCallback?
        external get() {  }
        external set(value: GDExtensionMainLoopShutdownCallback?) {  }

    var frame_func: GDExtensionMainLoopFrameCallback?
        external get() {  }
        external set(value: GDExtensionMainLoopFrameCallback?) {  }
}

enum class GDExtensionVariantType private constructor(value: UInt) : Enum<GDExtensionVariantType>, CEnum {
    GDEXTENSION_VARIANT_TYPE_NIL,

    GDEXTENSION_VARIANT_TYPE_BOOL,

    GDEXTENSION_VARIANT_TYPE_INT,

    GDEXTENSION_VARIANT_TYPE_FLOAT,

    GDEXTENSION_VARIANT_TYPE_STRING,

    GDEXTENSION_VARIANT_TYPE_VECTOR2,

    GDEXTENSION_VARIANT_TYPE_VECTOR2I,

    GDEXTENSION_VARIANT_TYPE_RECT2,

    GDEXTENSION_VARIANT_TYPE_RECT2I,

    GDEXTENSION_VARIANT_TYPE_VECTOR3,

    GDEXTENSION_VARIANT_TYPE_VECTOR3I,

    GDEXTENSION_VARIANT_TYPE_TRANSFORM2D,

    GDEXTENSION_VARIANT_TYPE_VECTOR4,

    GDEXTENSION_VARIANT_TYPE_VECTOR4I,

    GDEXTENSION_VARIANT_TYPE_PLANE,

    GDEXTENSION_VARIANT_TYPE_QUATERNION,

    GDEXTENSION_VARIANT_TYPE_AABB,

    GDEXTENSION_VARIANT_TYPE_BASIS,

    GDEXTENSION_VARIANT_TYPE_TRANSFORM3D,

    GDEXTENSION_VARIANT_TYPE_PROJECTION,

    GDEXTENSION_VARIANT_TYPE_COLOR,

    GDEXTENSION_VARIANT_TYPE_STRING_NAME,

    GDEXTENSION_VARIANT_TYPE_NODE_PATH,

    GDEXTENSION_VARIANT_TYPE_RID,

    GDEXTENSION_VARIANT_TYPE_OBJECT,

    GDEXTENSION_VARIANT_TYPE_CALLABLE,

    GDEXTENSION_VARIANT_TYPE_SIGNAL,

    GDEXTENSION_VARIANT_TYPE_DICTIONARY,

    GDEXTENSION_VARIANT_TYPE_ARRAY,

    GDEXTENSION_VARIANT_TYPE_PACKED_BYTE_ARRAY,

    GDEXTENSION_VARIANT_TYPE_PACKED_INT32_ARRAY,

    GDEXTENSION_VARIANT_TYPE_PACKED_INT64_ARRAY,

    GDEXTENSION_VARIANT_TYPE_PACKED_FLOAT32_ARRAY,

    GDEXTENSION_VARIANT_TYPE_PACKED_FLOAT64_ARRAY,

    GDEXTENSION_VARIANT_TYPE_PACKED_STRING_ARRAY,

    GDEXTENSION_VARIANT_TYPE_PACKED_VECTOR2_ARRAY,

    GDEXTENSION_VARIANT_TYPE_PACKED_VECTOR3_ARRAY,

    GDEXTENSION_VARIANT_TYPE_PACKED_COLOR_ARRAY,

    GDEXTENSION_VARIANT_TYPE_PACKED_VECTOR4_ARRAY,

    GDEXTENSION_VARIANT_TYPE_VARIANT_MAX;

    companion object {
        @Deprecated fun byValue(value: UInt): GDExtensionVariantType {  }
    }

    open val value: UInt
        open get() {  }

    class Var constructor(rawPtr: NativePtr) : CEnumVar {
        @internal.CEnumVarTypeSize @Deprecated companion object : CPrimitiveVar.Type {
        }

        var value: GDExtensionVariantType
            external get() {  }
            external set(value: GDExtensionVariantType) {  }
    }
}

enum class GDExtensionVariantOperator private constructor(value: UInt) : Enum<GDExtensionVariantOperator>, CEnum {
    GDEXTENSION_VARIANT_OP_EQUAL,

    GDEXTENSION_VARIANT_OP_NOT_EQUAL,

    GDEXTENSION_VARIANT_OP_LESS,

    GDEXTENSION_VARIANT_OP_LESS_EQUAL,

    GDEXTENSION_VARIANT_OP_GREATER,

    GDEXTENSION_VARIANT_OP_GREATER_EQUAL,

    GDEXTENSION_VARIANT_OP_ADD,

    GDEXTENSION_VARIANT_OP_SUBTRACT,

    GDEXTENSION_VARIANT_OP_MULTIPLY,

    GDEXTENSION_VARIANT_OP_DIVIDE,

    GDEXTENSION_VARIANT_OP_NEGATE,

    GDEXTENSION_VARIANT_OP_POSITIVE,

    GDEXTENSION_VARIANT_OP_MODULE,

    GDEXTENSION_VARIANT_OP_POWER,

    GDEXTENSION_VARIANT_OP_SHIFT_LEFT,

    GDEXTENSION_VARIANT_OP_SHIFT_RIGHT,

    GDEXTENSION_VARIANT_OP_BIT_AND,

    GDEXTENSION_VARIANT_OP_BIT_OR,

    GDEXTENSION_VARIANT_OP_BIT_XOR,

    GDEXTENSION_VARIANT_OP_BIT_NEGATE,

    GDEXTENSION_VARIANT_OP_AND,

    GDEXTENSION_VARIANT_OP_OR,

    GDEXTENSION_VARIANT_OP_XOR,

    GDEXTENSION_VARIANT_OP_NOT,

    GDEXTENSION_VARIANT_OP_IN,

    GDEXTENSION_VARIANT_OP_MAX;

    companion object {
        @Deprecated fun byValue(value: UInt): GDExtensionVariantOperator {  }
    }

    open val value: UInt
        open get() {  }

    class Var constructor(rawPtr: NativePtr) : CEnumVar {
        @internal.CEnumVarTypeSize @Deprecated companion object : CPrimitiveVar.Type {
        }

        var value: GDExtensionVariantOperator
            external get() {  }
            external set(value: GDExtensionVariantOperator) {  }
    }
}

enum class GDExtensionCallErrorType private constructor(value: UInt) : Enum<GDExtensionCallErrorType>, CEnum {
    GDEXTENSION_CALL_OK,

    GDEXTENSION_CALL_ERROR_INVALID_METHOD,

    GDEXTENSION_CALL_ERROR_INVALID_ARGUMENT,

    GDEXTENSION_CALL_ERROR_TOO_MANY_ARGUMENTS,

    GDEXTENSION_CALL_ERROR_TOO_FEW_ARGUMENTS,

    GDEXTENSION_CALL_ERROR_INSTANCE_IS_NULL,

    GDEXTENSION_CALL_ERROR_METHOD_NOT_CONST;

    companion object {
        @Deprecated fun byValue(value: UInt): GDExtensionCallErrorType {  }
    }

    open val value: UInt
        open get() {  }

    class Var constructor(rawPtr: NativePtr) : CEnumVar {
        @internal.CEnumVarTypeSize @Deprecated companion object : CPrimitiveVar.Type {
        }

        var value: GDExtensionCallErrorType
            external get() {  }
            external set(value: GDExtensionCallErrorType) {  }
    }
}

enum class GDExtensionClassMethodFlags private constructor(value: UInt) : Enum<GDExtensionClassMethodFlags>, CEnum {
    GDEXTENSION_METHOD_FLAG_NORMAL,

    GDEXTENSION_METHOD_FLAG_EDITOR,

    GDEXTENSION_METHOD_FLAG_CONST,

    GDEXTENSION_METHOD_FLAG_VIRTUAL,

    GDEXTENSION_METHOD_FLAG_VARARG,

    GDEXTENSION_METHOD_FLAG_STATIC,

    GDEXTENSION_METHOD_FLAG_VIRTUAL_REQUIRED;

    companion object {
        @internal.CEnumEntryAlias val GDEXTENSION_METHOD_FLAGS_DEFAULT: GDExtensionClassMethodFlags
            get() {  }

        @Deprecated fun byValue(value: UInt): GDExtensionClassMethodFlags {  }
    }

    open val value: UInt
        open get() {  }

    class Var constructor(rawPtr: NativePtr) : CEnumVar {
        @internal.CEnumVarTypeSize @Deprecated companion object : CPrimitiveVar.Type {
        }

        var value: GDExtensionClassMethodFlags
            external get() {  }
            external set(value: GDExtensionClassMethodFlags) {  }
    }
}

enum class GDExtensionClassMethodArgumentMetadata private constructor(value: UInt) : Enum<GDExtensionClassMethodArgumentMetadata>, CEnum {
    GDEXTENSION_METHOD_ARGUMENT_METADATA_NONE,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_INT8,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_INT16,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_INT32,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_INT64,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_UINT8,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_UINT16,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_UINT32,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_UINT64,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_REAL_IS_FLOAT,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_REAL_IS_DOUBLE,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_CHAR16,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_CHAR32,

    GDEXTENSION_METHOD_ARGUMENT_METADATA_OBJECT_IS_REQUIRED;

    companion object {
        @Deprecated fun byValue(value: UInt): GDExtensionClassMethodArgumentMetadata {  }
    }

    open val value: UInt
        open get() {  }

    class Var constructor(rawPtr: NativePtr) : CEnumVar {
        @internal.CEnumVarTypeSize @Deprecated companion object : CPrimitiveVar.Type {
        }

        var value: GDExtensionClassMethodArgumentMetadata
            external get() {  }
            external set(value: GDExtensionClassMethodArgumentMetadata) {  }
    }
}

enum class GDExtensionInitializationLevel private constructor(value: UInt) : Enum<GDExtensionInitializationLevel>, CEnum {
    GDEXTENSION_INITIALIZATION_CORE,

    GDEXTENSION_INITIALIZATION_SERVERS,

    GDEXTENSION_INITIALIZATION_SCENE,

    GDEXTENSION_INITIALIZATION_EDITOR,

    GDEXTENSION_MAX_INITIALIZATION_LEVEL;

    companion object {
        @Deprecated fun byValue(value: UInt): GDExtensionInitializationLevel {  }
    }

    open val value: UInt
        open get() {  }

    class Var constructor(rawPtr: NativePtr) : CEnumVar {
        @internal.CEnumVarTypeSize @Deprecated companion object : CPrimitiveVar.Type {
        }

        var value: GDExtensionInitializationLevel
            external get() {  }
            external set(value: GDExtensionInitializationLevel) {  }
    }
}

```
