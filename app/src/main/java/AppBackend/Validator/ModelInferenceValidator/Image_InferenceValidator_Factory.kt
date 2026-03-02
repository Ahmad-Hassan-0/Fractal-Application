package AppBackend.Validator.ModelInferenceValidator

class Image_InferenceValidator_Factory: InferenceValidator_Factory {
    override fun createInferenceValidator(): InferenceValidator {
        return Image_InferenceValidator()
    }
}