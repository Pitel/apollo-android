package test

import com.apollographql.apollo3.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo3.testing.readFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import kotlin.test.assertEquals

/**
 * These tests read the operationOutput.json generated during compilation and compare it to the generated models
 *
 * This makes sure minification is the same between operationOutput.json and the models
 *
 * This is a JVM only test because we need to assume "http-kotlin" for the service name
 * where the file will be generated. Apple code shouldn't be much different in all cases
 */
class OperationOutputTest {
  @Test
  fun operationOutputMatchesTheModels() {
    val operationOutput = readFile("build/generated/operationOutput/apollo/httpcache-kotlin/operationOutput.json")
    val source = Json.parseToJsonElement(operationOutput).jsonObject.entries.mapNotNull {
      val descriptor = it.value.jsonObject
      if (descriptor.getValue("name").jsonPrimitive.content == "AllFilms") {
        descriptor.getValue("source").jsonPrimitive.content
      } else {
        null
      }
    }.single()

    assertEquals(AllFilmsQuery().document(), source)
  }
}