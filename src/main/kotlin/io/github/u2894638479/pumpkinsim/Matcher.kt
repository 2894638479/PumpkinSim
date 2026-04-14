package io.github.u2894638479.pumpkinsim

class Matcher(
    private val graph: List<IntArray>,
    private val vSize: Int
) {

    private val matchV = IntArray(vSize) { -1 }
    private lateinit var visited: BooleanArray

    fun maxMatching(): Int {
        var result = 0
        for (u in graph.indices) {
            visited = BooleanArray(vSize)
            if (dfs(u)) result++
        }
        return result
    }

    private fun dfs(u: Int): Boolean {
        for (v in graph[u]) {
            if (visited[v]) continue
            visited[v] = true
            if (matchV[v] == -1 || dfs(matchV[v]) ) {
                matchV[v] = u
                return true
            }
        }
        return false
    }
}