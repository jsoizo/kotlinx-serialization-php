import com.codmon.serialization.php.PHP
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Files
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
    val D = '$'

    fun generateAssertionPHPScript(script: String): String {
        return """
            <?php
            ${D}fileName = ${D}argv[1];
            ${D}decodedData = null;
            
            // read file and unserialize
            function decodeFile() {
                global ${D}fileName, ${D}decodedData;
                echo "Decoding file: ${D}fileName\n";
                ${D}encodedData = file_get_contents(${D}fileName);
                ${D}decodedData = unserialize(${D}encodedData);
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
            ${D}encodedFileName = ${D}argv[1];
             
            function encodeAndSaveToFile(${D}data) {
                global ${D}encodedFileName;
                ${D}encodedData = serialize(${D}data);
                file_put_contents(${D}encodedFileName, ${D}encodedData);
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
                public ${D}string = "Hello, World!";
                public ${D}int = 42;
                public ${D}long = 9223372036854775807;
                public ${D}float = 3.4028235E38;
                public ${D}double = 1.7976931348623157E308;
                public ${D}boolean = true;
                public ${D}list = array("a", "b", "c");
                public ${D}map = array("x" => 1, "y" => 2, "z" => 3);
                public ${D}nullableString = null;
                public ${D}char = 'K';
            }
            decodeFile();
            assert(is_object(${D}decodedData));
            assert(${D}decodedData instanceof TestClass);
            assert(${D}decodedData == new TestClass());
            """.trimIndent()
        val assertEnum =
            """
            enum TestEnum {
                case ONE;
                case TWO;
                case THREE;
            }
            decodeFile();
            assert(${D}decodedData === TestEnum::TWO);
            """.trimIndent()
        val assertSealed =
            """
            class SubClass1 {
                public ${D}value = 100;
            }
            decodeFile();
            assert(is_object(${D}decodedData));
            assert(${D}decodedData instanceof SubClass1);
            assert(${D}decodedData == new SubClass1());
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

    @Test
    fun testPHPEncodeKotlinDecode() {
        // Generate PHP script for encoding
        val encodeClass =
            """
            class TestClass {
                public ${D}string = "Hello from PHP!";
                public ${D}int = 100;
                public ${D}long = 9876543210;
                public ${D}float = 3.14;
                public ${D}double = 3.14159;
                public ${D}boolean = false;
                public ${D}list = array("php", "kotlin", "interop");
                public ${D}map = array("a" => 1, "b" => 2, "c" => 3);
                public ${D}nullableString = 'Not Null';
                public ${D}char = 'P';
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
