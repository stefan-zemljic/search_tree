package ch.bytecraft.search_tree

class SearchTree<T>(private val key: T.() -> String) {
    sealed class Query {
        class Combined(val queries: List<Query>) : Query() {
            constructor(vararg queries: Query) : this(queries.toList())
        }

        class Literal(val value: String) : Query()
        object AnyChar : Query()
        object AnyString : Query()
    }

    private class Node<T>(val parent: Node<T>?, val char: Char? = null) {
        val children = mutableMapOf<Char, Node<T>>()
        val values = mutableSetOf<Pair<String, T>>()

        fun appendTo(builder: StringBuilder, prefix: String, isTail: Boolean) {
            builder.append(prefix)
            builder.append(if (isTail) "└── " else "├── ")
            builder.append(char ?: "root")
            if (values.isNotEmpty()) {
                builder.append(" [")
                builder.append(
                    values.map { it.first }
                        .toSet().sorted()
                        .joinToString(", ")
                )
                builder.append("]")
            }
            builder.append("\n")
            val childrenList = children.values.toList().sortedBy { it.char }
            for (i in childrenList.indices) {
                val child = childrenList[i]
                val isLast = i == childrenList.size - 1
                child.appendTo(
                    builder,
                    prefix + if (isTail) "    " else "│   ",
                    isLast
                )
            }
        }
    }

    private val root = Node<T>(null)

    fun add(value: T) {
        val key = value.key()
        val pair = key to value
        var node = root
        for (char in key) {
            node = node.children.getOrPut(char) { Node(node, char) }
        }
        node.values.add(pair)
    }

    fun remove(value: T): Boolean {
        val key = value.key()
        var node = root
        for (char in key) {
            node = node.children[char] ?: return false
        }
        val pair = key to value
        while (true) {
            node.values.remove(pair)
            if (node.values.isNotEmpty() || node.children.isNotEmpty()) break
            val parent = node.parent ?: break
            parent.children.remove(node.char!!)
            node = parent
        }
        return true
    }

    fun search(vararg queries: Query): List<T> {
        return search(Query.Combined(*queries))
    }

    fun search(query: Query): List<T> {
        var currentNodes = mutableListOf(root)
        var nextNodes = mutableListOf<Node<T>>()
        val todo: MutableList<Query> = mutableListOf(query)
        outer@ while (todo.isNotEmpty()) {
            val currentQuery = todo.removeLast()
            for (currentNode in currentNodes) {
                when (currentQuery) {
                    is Query.Literal -> {
                        var node: Node<T>? = currentNode
                        for (char in currentQuery.value) {
                            node = node!!.children[char]
                            if (node == null) break
                        }
                        node?.let(nextNodes::add)
                    }
                    Query.AnyChar -> nextNodes.addAll(currentNode.children.values)
                    Query.AnyString -> {
                        val currentTodos = mutableListOf(currentNode)
                        while (currentTodos.isNotEmpty()) {
                            val current = currentTodos.removeLast()
                            nextNodes.add(current)
                            for (child in current.children.values) {
                                currentTodos.add(child)
                            }
                        }
                    }
                    is Query.Combined -> {
                        val queries = currentQuery.queries
                        var i = queries.lastIndex
                        while (i >= 0) todo.add(queries[i--])
                        continue@outer
                    }
                }
            }
            val tmp = currentNodes
            currentNodes = nextNodes
            nextNodes = tmp
            nextNodes.clear()
        }
        return currentNodes.flatMap { it.values }.toSet().sortedBy { it.first }.map { it.second }
    }


    override fun toString(): String {
        return buildString { root.appendTo(this, "", true) }
    }
}