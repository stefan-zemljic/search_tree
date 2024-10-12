package ch.bytecraft.search_tree

import ch.bytecraft.search_tree.SearchTree.Query.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SearchTreeTest {
    private lateinit var searchTree: SearchTree<TestData>

    data class TestData(val name: String)

    @BeforeEach
    fun setUp() {
        searchTree = SearchTree { name }
    }

    private fun addTestData(vararg names: String) {
        names.forEach { searchTree.add(TestData(it)) }
    }

    @Test
    fun `test add and search with single entry`() {
        addTestData("test")
        val result = searchTree.search(Literal("test"))
        assertEquals(1, result.size)
        assertEquals("test", result[0].name)
    }

    @Test
    fun `test add and search with multiple entries`() {
        addTestData("apple", "banana", "grape")
        val result = searchTree.search(AnyString, Literal("ap"), AnyString)
        assertEquals(2, result.size)
        assertEquals("apple", result[0].name)
        assertEquals("grape", result[1].name)
    }

    @Test
    fun `test search with no matches`() {
        addTestData("apple", "banana", "grape")
        val result = searchTree.search(Literal("orange"))
        assertEquals(0, result.size)
    }

    @Test
    fun `test search with empty string`() {
        addTestData("apple", "banana", "grape")
        val result = searchTree.search(AnyString)
        assertEquals(3, result.size)
    }

    @Test
    fun `test add and remove`() {
        addTestData("test", "apple")
        searchTree.remove(TestData("test"))
        val result = searchTree.search(Literal("test"))
        assertEquals(0, result.size)
    }

    @Test
    fun `test add and remove non-existent`() {
        addTestData("test", "apple")
        searchTree.remove(TestData("banana"))
        val result = searchTree.search(Literal("test"))
        assertEquals(1, result.size)
        assertEquals("test", result[0].name)
    }

    @Test
    fun `test multiple values with the same prefix`() {
        addTestData("apple", "app", "apricot")
        val result = searchTree.search(Literal("ap"), AnyString)
        assertEquals(3, result.size)
        assertEquals(listOf("app", "apple", "apricot"), result.map { it.name })
    }

    @Test
    fun `test empty searchable`() {
        val result = searchTree.search(Literal("anything"))
        assertEquals(0, result.size)
    }

    @Test
    fun `test adding duplicate entries`() {
        val testData = TestData("test")
        searchTree.add(testData)
        searchTree.add(testData)
        val result = searchTree.search(Literal("test"))
        assertEquals(1, result.size)
        assertEquals("test", result[0].name)
    }

    @Test
    fun `test loops with 0, 1, and multiple iterations`() {
        assertEquals(0, searchTree.search(Literal("any")).size)

        addTestData("a")
        assertEquals(1, searchTree.search(Literal("a")).size)

        addTestData("abc", "abcd", "abcde")
        assertEquals(3, searchTree.search(Literal("abc"), AnyString).size)
    }

    @Test
    fun `test toString representation`() {
        addTestData("apple", "banana", "grape")
        val result = searchTree.toString()
        assert(result.contains("a")) { "Expected 'a' in $result" }
        assert(result.contains("b")) { "Expected 'b' in $result" }
        assert(result.contains("g")) { "Expected 'g' in $result" }
        assert(result.contains("apple")) { "Expected 'apple' in $result" }
        assert(result.contains("banana")) { "Expected 'banana' in $result" }
        assert(result.contains("grape")) { "Expected 'grape' in $result" }
    }

    @Test
    fun `test remove with single entry`() {
        addTestData("test")
        searchTree.remove(TestData("test"))
        val result = searchTree.search(Literal("test"))
        assertEquals(0, result.size)
    }

    @Test
    fun `AnyChar skips exactly one char`() {
        addTestData("abcd", "abd", "abcdd", "abdd", "abccd")
        val result = searchTree.search(Literal("ab"), AnyChar, Literal("d"))
        assertThat(result).containsExactly(TestData("abcd"), TestData("abdd"))
    }

    @Test
    fun `AnyString skips any number of chars`() {
        addTestData("abcd", "abd", "abcdd", "abdd", "abccd", "ab", "ad")
        val result = searchTree.search(Literal("ab"), AnyString, Literal("d"))
        assertThat(result).containsExactlyInAnyOrder(
            TestData("abcd"),
            TestData("abd"),
            TestData("abdd"),
            TestData("abccd"),
            TestData("abcdd"),
        )
    }

    @Test
    fun `Does not remove nodes if there are other paths`() {
        addTestData("abcd", "abce")
        searchTree.remove(TestData("abce"))
        assertEquals(1, searchTree.search(Literal("abcd")).size)
        assertEquals(0, searchTree.search(Literal("abce")).size)
    }

    @Test
    fun `Does not remove nodes if the nodes is still an end`() {
        addTestData("abc", "abce")
        searchTree.remove(TestData("abce"))
        assertEquals(1, searchTree.search(Literal("abc")).size)
        assertEquals(0, searchTree.search(Literal("abce")).size)
    }
}
