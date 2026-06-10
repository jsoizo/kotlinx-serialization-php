import com.jsoizo.serialization.php.PHP
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Files
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PHPInteroperabilityTest {
    private val phpExecutablePath: String
    private val testResourcesPath: String

    init {
        val envPath = System.getenv("PHP_EXECUTABLE_PATH")
        phpExecutablePath =
            if (envPath != null) {
                envPath
            } else {
                val properties = Properties()
                this.javaClass.classLoader.getResourceAsStream("application.properties").use(properties::load)
                properties.getProperty("php.executable.path")
            }
        testResourcesPath = createTempDirectory()
    }

    private fun createTempDirectory(): String {
        return Files.createTempDirectory("php_interop_test").toString()
    }

    @AfterTest
    fun cleanup() {
        File(testResourcesPath).deleteRecursively()
    }

    private fun executePhpScript(
        scriptName: String,
        vararg args: String,
    ) {
        val scriptPath = File(testResourcesPath, scriptName).absolutePath
        val process =
            ProcessBuilder(phpExecutablePath, scriptPath, *args)
                .directory(File(testResourcesPath))
                .redirectErrorStream(true)
                .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        println("PHP Script Output for $scriptName:")
        println(output)

        if (exitCode != 0) {
            throw RuntimeException("PHP script execution failed with exit code $exitCode. Output: $output")
        }

        println("PHP script executed successfully")
    }

    @Serializable
    data class TestClass(
        val string: String,
        val int: Int,
        val long: Long,
        val float: Float,
        val double: Double,
        val boolean: Boolean,
        val list: List<String>,
        val map: Map<String, Int>,
        val nullableString: String?,
        val char: Char,
    )

    @Serializable
    enum class TestEnum(val value: String) {
        ONE("one"),
        TWO("two"),
        THREE("three"),
    }

    @Serializable
    sealed class TestSealed {
        @Serializable
        data class SubClass1(val value: Int) : TestSealed()

        @Serializable
        data class SubClass2(val value: String) : TestSealed()
    }

    // For escaping dollar sign in PHP script
    private val dollar = '$'

    fun generateAssertionPHPScript(script: String): String {
        return """
            <?php
            ${dollar}fileName = ${dollar}argv[1];
            ${dollar}decodedData = null;
            
            // read file and unserialize
            function decodeFile() {
                global ${dollar}fileName, ${dollar}decodedData;
                echo "Decoding file: ${dollar}fileName\n";
                ${dollar}encodedData = file_get_contents(${dollar}fileName);
                ${dollar}decodedData = unserialize(${dollar}encodedData);
                echo "Decoding process completed.\n";
            }
            
            // assert that the decoded data is equal to the expected data
            $script
            ?>
            """.trimIndent()
    }

    fun generateEncodePHPScript(script: String): String {
        return """
            <?php
            ${dollar}encodedFileName = ${dollar}argv[1];
             
            function encodeAndSaveToFile(${dollar}data) {
                global ${dollar}encodedFileName;
                ${dollar}encodedData = serialize(${dollar}data);
                file_put_contents(${dollar}encodedFileName, ${dollar}encodedData);
            }
            
            $script
            ?>
            """.trimIndent()
    }

    @Test
    fun testKotlinEncodePHPDecode() {
        val testData =
            TestClass(
                string = "Hello, World!",
                int = 42,
                long = Long.MAX_VALUE,
                float = Float.MAX_VALUE,
                double = Double.MAX_VALUE,
                boolean = true,
                list = listOf("a", "b", "c"),
                map = mapOf("x" to 1, "y" to 2, "z" to 3),
                nullableString = null,
                char = 'K',
            )

        val testEnum = TestEnum.TWO
        val testSealed: TestSealed = TestSealed.SubClass1(100)

        // Encode by Kotlin
        val encodedClass = PHP.encodeToString(testData)
        val encodedEnum = PHP.encodeToString(testEnum)
        val encodedSealed = PHP.encodeToString(testSealed)

        // Write encoded data to files
        File(testResourcesPath, "encoded_class.txt").writeText(encodedClass)
        File(testResourcesPath, "encoded_enum.txt").writeText(encodedEnum)
        File(testResourcesPath, "encoded_sealed.txt").writeText(encodedSealed)

        // Generate PHP script for assertions
        val assertClass =
            """
            class TestClass {
                public ${dollar}string = "Hello, World!";
                public ${dollar}int = 42;
                public ${dollar}long = 9223372036854775807;
                public ${dollar}float = 3.4028235E38;
                public ${dollar}double = 1.7976931348623157E308;
                public ${dollar}boolean = true;
                public ${dollar}list = array("a", "b", "c");
                public ${dollar}map = array("x" => 1, "y" => 2, "z" => 3);
                public ${dollar}nullableString = null;
                public ${dollar}char = 'K';
            }
            decodeFile();
            assert(is_object(${dollar}decodedData));
            assert(${dollar}decodedData instanceof TestClass);
            assert(${dollar}decodedData == new TestClass());
            """.trimIndent()
        val assertEnum =
            """
            enum TestEnum {
                case ONE;
                case TWO;
                case THREE;
            }
            decodeFile();
            assert(${dollar}decodedData === TestEnum::TWO);
            """.trimIndent()
        val assertSealed =
            """
            class SubClass1 {
                public ${dollar}value = 100;
            }
            decodeFile();
            assert(is_object(${dollar}decodedData));
            assert(${dollar}decodedData instanceof SubClass1);
            assert(${dollar}decodedData == new SubClass1());
            """.trimIndent()
        val assertionScriptClass = generateAssertionPHPScript(assertClass)
        val assertionScriptEnum = generateAssertionPHPScript(assertEnum)
        val assertionScriptSealed = generateAssertionPHPScript(assertSealed)

        // Write assertion scripts to files
        File(testResourcesPath, "assert_class.php").writeText(assertionScriptClass)
        File(testResourcesPath, "assert_enum.php").writeText(assertionScriptEnum)
        File(testResourcesPath, "assert_sealed.php").writeText(assertionScriptSealed)

        // Execute PHP script to assert the decoded data
        executePhpScript("assert_class.php", "encoded_class.txt")
        executePhpScript("assert_enum.php", "encoded_enum.txt")
        executePhpScript("assert_sealed.php", "encoded_sealed.txt")
    }

    @Serializable
    data class NonAsciiData(
        val text: String,
        val char: Char,
    )

    @Test
    fun testKotlinEncodePHPDecodeNonAscii() {
        val encoded = PHP.encodeToString(NonAsciiData("こんにちは世界", 'あ'))
        File(testResourcesPath, "encoded_non_ascii.txt").writeText(encoded)

        val assertScript =
            """
            class NonAsciiData {
                public ${dollar}text;
                public ${dollar}char;
            }
            decodeFile();
            assert(${dollar}decodedData->text === "こんにちは世界");
            assert(${dollar}decodedData->char === "あ");
            """.trimIndent()
        File(testResourcesPath, "assert_non_ascii.php").writeText(generateAssertionPHPScript(assertScript))

        executePhpScript("assert_non_ascii.php", "encoded_non_ascii.txt")
    }

    @Test
    fun testPHPEncodeKotlinDecodeNonAscii() {
        val encodeScript =
            """
            class NonAsciiData {
                public ${dollar}text = "こんにちは世界";
                public ${dollar}char = "あ";
            }
            encodeAndSaveToFile(new NonAsciiData());
            """.trimIndent()
        File(testResourcesPath, "encode_non_ascii.php").writeText(generateEncodePHPScript(encodeScript))
        executePhpScript("encode_non_ascii.php", "php_encoded_non_ascii.txt")

        val encoded = File(testResourcesPath, "php_encoded_non_ascii.txt").readText()
        val decoded = PHP.decodeFromString<NonAsciiData>(encoded)

        assertEquals(NonAsciiData("こんにちは世界", 'あ'), decoded)
    }

    @Serializable
    data class SpecialFloats(
        val nan: Double,
        val inf: Double,
        val negInf: Double,
    )

    @Test
    fun testKotlinEncodePHPDecodeSpecialFloats() {
        val encoded = PHP.encodeToString(SpecialFloats(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
        File(testResourcesPath, "encoded_special_floats.txt").writeText(encoded)

        val assertScript =
            """
            class SpecialFloats {
                public ${dollar}nan;
                public ${dollar}inf;
                public ${dollar}negInf;
            }
            decodeFile();
            assert(is_nan(${dollar}decodedData->nan));
            assert(${dollar}decodedData->inf === INF);
            assert(${dollar}decodedData->negInf === -INF);
            """.trimIndent()
        File(testResourcesPath, "assert_special_floats.php").writeText(generateAssertionPHPScript(assertScript))

        executePhpScript("assert_special_floats.php", "encoded_special_floats.txt")
    }

    @Test
    fun testPHPEncodeKotlinDecodeSpecialFloats() {
        val encodeScript =
            """
            class SpecialFloats {
                public ${dollar}nan = NAN;
                public ${dollar}inf = INF;
                public ${dollar}negInf = -INF;
            }
            encodeAndSaveToFile(new SpecialFloats());
            """.trimIndent()
        File(testResourcesPath, "encode_special_floats.php").writeText(generateEncodePHPScript(encodeScript))
        executePhpScript("encode_special_floats.php", "php_encoded_special_floats.txt")

        val encoded = File(testResourcesPath, "php_encoded_special_floats.txt").readText()
        val decoded = PHP.decodeFromString<SpecialFloats>(encoded)

        assertTrue(decoded.nan.isNaN())
        assertEquals(Double.POSITIVE_INFINITY, decoded.inf)
        assertEquals(Double.NEGATIVE_INFINITY, decoded.negInf)
    }

    @Serializable
    data class VisibilityClass(
        val publicProp: String,
        val protectedProp: Int,
        val privateProp: Boolean,
    )

    @Test
    fun testPHPEncodeKotlinDecodeNonPublicProperties() {
        // PHP mangles protected/private property names in serialized output;
        // the decoder must match them by their bare names.
        val encodeVisibility =
            """
            class VisibilityClass {
                public ${dollar}publicProp = "pub";
                protected ${dollar}protectedProp = 42;
                private ${dollar}privateProp = true;
            }
            encodeAndSaveToFile(new VisibilityClass());
            """.trimIndent()

        File(testResourcesPath, "encode_visibility.php").writeText(generateEncodePHPScript(encodeVisibility))
        executePhpScript("encode_visibility.php", "php_encoded_visibility.txt")

        val encoded = File(testResourcesPath, "php_encoded_visibility.txt").readText()
        val decoded = PHP.decodeFromString<VisibilityClass>(encoded)

        assertEquals(VisibilityClass("pub", 42, true), decoded)
    }

    @Test
    fun testPHPEncodeKotlinDecode() {
        // Generate PHP script for encoding
        val encodeClass =
            """
            class TestClass {
                public ${dollar}string = "Hello from PHP!";
                public ${dollar}int = 100;
                public ${dollar}long = 9876543210;
                public ${dollar}float = 3.14;
                public ${dollar}double = 3.14159;
                public ${dollar}boolean = false;
                public ${dollar}list = array("php", "kotlin", "interop");
                public ${dollar}map = array("a" => 1, "b" => 2, "c" => 3);
                public ${dollar}nullableString = 'Not Null';
                public ${dollar}char = 'P';
            }
            encodeAndSaveToFile(new TestClass());
            """.trimIndent()
        val encodeEnum =
            """
            enum TestEnum {
                case ONE;
                case TWO;
                case THREE;
            }
            encodeAndSaveToFile(TestEnum::ONE);
            """.trimIndent()

        // Write PHP scripts to files
        File(testResourcesPath, "encode_class.php").writeText(generateEncodePHPScript(encodeClass))
        File(testResourcesPath, "encode_enum.php").writeText(generateEncodePHPScript(encodeEnum))

        // Execute PHP script to encode data
        executePhpScript("encode_class.php", "php_encoded_class.txt")
        executePhpScript("encode_enum.php", "php_encoded_enum.txt")

        // Read encoded data from PHP script output
        val phpEncodedClass = File(testResourcesPath, "php_encoded_class.txt").readText()
        val phpEncodedEnum = File(testResourcesPath, "php_encoded_enum.txt").readText()

        // Decode by Kotlin
        val decodedClass = PHP.decodeFromString<TestClass>(phpEncodedClass)
        val decodedEnum = PHP.decodeFromString<TestEnum>(phpEncodedEnum)

        // Expected data
        val expectedClass =
            TestClass(
                string = "Hello from PHP!",
                int = 100,
                long = 9876543210L,
                float = 3.14f,
                double = 3.14159,
                boolean = false,
                list = listOf("php", "kotlin", "interop"),
                map = mapOf("a" to 1, "b" to 2, "c" to 3),
                nullableString = "Not Null",
                char = 'P',
            )
        val expectedEnum = TestEnum.ONE

        // Assert decoded data
        assertEquals(expectedClass, decodedClass, "Kotlin decoded class should match the PHP encoded data")
        assertEquals(expectedEnum, decodedEnum, "Kotlin decoded enum should match the PHP encoded data")
    }
}
