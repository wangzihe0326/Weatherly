package au.edu.jcu.assessment.utilityapp.data.remote

import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import java.io.EOFException
import java.lang.reflect.Type
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Normalizes weather JSON parsing failures while preserving Retrofit's transport error handling. */
class WeatherResponseConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        if (type != WeatherResponse::class.java) return null
        val delegate = GsonConverterFactory.create().responseBodyConverter(type, annotations, retrofit)
            ?: return null
        return Converter<ResponseBody, Any?> { body ->
            // Retrofit restores recorded body-read IO failures when conversion throws a runtime error.
            try {
                delegate.convert(body)
            } catch (failure: EOFException) {
                throw JsonSyntaxException(failure)
            } catch (failure: MalformedJsonException) {
                throw JsonSyntaxException(failure)
            } catch (failure: NumberFormatException) {
                throw JsonSyntaxException(failure)
            }
        }
    }
}
