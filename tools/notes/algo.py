# -*- coding: utf-8 -*-
"""Algorithms revision notes."""

NOTES = {
    "algo_algorithms": {
        "title": "Introduction to Algorithms",
        "blocks": [
            ("def", "An algorithm is a finite, unambiguous sequence of steps that turns an input into the intended output and always terminates."),
            ("p", "GATE almost never asks you to invent an algorithm. It asks how a known one behaves: how many comparisons, how much extra memory, what happens on the worst input."),
            ("h", "What to measure"),
            ("bullets", [
                "Time: the count of primitive operations as a function of input size, not seconds.",
                "Space: extra memory beyond the input itself.",
                "Correctness: usually argued by a loop invariant or by induction on the recursion.",
            ]),
            ("note", "Machine speed and language are deliberately ignored. Doubling the processor speed halves the running time of every algorithm and so tells you nothing about which one to pick."),
            ("key", "Worst case is the default in GATE unless the question says otherwise. Average case needs an assumed input distribution, and best case is rarely informative."),
        ],
    },

    "algo_asymptotic": {
        "title": "Asymptotic Notations",
        "blocks": [
            ("p", "Asymptotic notation compares growth rates and throws away constants, because constants depend on the machine and growth does not."),
            ("table", [
                ["Notation", "Meaning", "Reads as"],
                ["O(g)", "f ≤ c·g for large n", "at most, upper bound"],
                ["Ω(g)", "f ≥ c·g for large n", "at least, lower bound"],
                ["Θ(g)", "both of the above", "exactly this order"],
                ["o(g)", "f/g → 0", "strictly smaller"],
                ["ω(g)", "f/g → ∞", "strictly larger"],
            ]),
            ("formula", "f(n) = O(g(n)) ⟺ ∃ c > 0, n₀ such that 0 ≤ f(n) ≤ c·g(n) for all n ≥ n₀"),
            ("example", "3n² + 5n log n + 100 is Θ(n²). The n log n and constant terms are dominated, and the factor 3 is absorbed into c."),
            ("h", "The order that matters"),
            ("formula", "1 < log n < √n < n < n log n < n² < n³ < 2ⁿ < n!"),
            ("warn", "O is an upper bound, not a promise of tightness. n is O(n²) is a true statement and a useless one, which is why questions asking for the tightest bound expect Θ."),
        ],
    },

    "algo_search": {
        "title": "Searching Algorithms",
        "blocks": [
            ("p", "Two searches matter for GATE, and the difference between them is whether the data is already ordered."),
            ("table", [
                ["", "Linear search", "Binary search"],
                ["Needs sorted input", "no", "yes"],
                ["Worst case", "O(n)", "O(log n)"],
                ["Best case", "O(1)", "O(1)"],
                ["Works on linked lists", "yes", "not usefully"],
            ]),
            ("key", "Binary search needs random access. On a linked list, reaching the middle already costs O(n), which destroys the advantage."),
            ("note", "Sorting to enable binary search costs O(n log n), so it pays only when many searches follow. For a single lookup, linear search wins."),
        ],
    },

    "algo_binearysearch": {
        "title": "Binary Search",
        "blocks": [
            ("p", "Halve the interval at every step: compare the middle element, discard the half that cannot contain the key."),
            ("code", ("c", "int lo = 0, hi = n - 1;\nwhile (lo <= hi) {\n    int mid = lo + (hi - lo) / 2;\n    if (a[mid] == key) return mid;\n    if (a[mid] < key) lo = mid + 1;\n    else hi = mid - 1;\n}\nreturn -1;")),
            ("complexity", ("Time complexity", "O(log n) worst and average, O(1) best")),
            ("formula", "T(n) = T(n/2) + O(1) ⟹ T(n) = Θ(log n)"),
            ("warn", "Writing mid = (lo + hi) / 2 overflows for large indices. lo + (hi - lo) / 2 is the same value without the overflow."),
            ("key", "The number of comparisons in the worst case is ⌊log₂ n⌋ + 1. A search tree drawn over the recursion has that height, which is where the bound comes from."),
        ],
    },

    "algo_sort": {
        "title": "Sorting Algorithms",
        "blocks": [
            ("p", "The comparison sorts differ in worst case, extra space and stability. GATE asks about all three."),
            ("table", [
                ["Algorithm", "Best", "Average", "Worst", "Space", "Stable"],
                ["Bubble", "O(n)", "O(n²)", "O(n²)", "O(1)", "yes"],
                ["Selection", "O(n²)", "O(n²)", "O(n²)", "O(1)", "no"],
                ["Insertion", "O(n)", "O(n²)", "O(n²)", "O(1)", "yes"],
                ["Merge", "O(n log n)", "O(n log n)", "O(n log n)", "O(n)", "yes"],
                ["Quick", "O(n log n)", "O(n log n)", "O(n²)", "O(log n)", "no"],
                ["Heap", "O(n log n)", "O(n log n)", "O(n log n)", "O(1)", "no"],
            ]),
            ("def", "A sort is stable when equal keys keep their original relative order. It matters when records are sorted on one field after another."),
            ("key", "No comparison sort can beat Ω(n log n). A decision tree on n items has n! leaves, so its height is at least log₂(n!) = Θ(n log n)."),
            ("note", "Counting, radix and bucket sort escape that bound only because they do not compare elements; they exploit the structure of the keys instead."),
        ],
    },

    "algo_bubblesort": {
        "title": "Bubble Sort",
        "blocks": [
            ("p", "Repeatedly sweep the array swapping adjacent elements that are out of order. After pass i, the largest i elements are in place."),
            ("code", ("c", "for (i = 0; i < n - 1; i++) {\n    swapped = 0;\n    for (j = 0; j < n - 1 - i; j++)\n        if (a[j] > a[j+1]) { swap(a[j], a[j+1]); swapped = 1; }\n    if (!swapped) break;\n}")),
            ("complexity", ("Time complexity", "O(n²) worst and average, O(n) best with the swapped flag")),
            ("key", "Without the early-exit flag the best case is also O(n²). Questions often hinge on whether the optimised version is meant."),
            ("note", "The number of swaps equals the number of inversions in the input, which is why bubble sort is sometimes used to count them."),
        ],
    },

    "algo_selectionsort": {
        "title": "Selection Sort",
        "blocks": [
            ("p", "Find the minimum of the unsorted part and swap it into position. Repeat for each position in turn."),
            ("complexity", ("Time complexity", "Θ(n²) in every case")),
            ("key", "Comparisons are always n(n−1)/2 regardless of the input, but swaps are at most n−1. That makes it attractive when writing is far more expensive than reading."),
            ("warn", "Selection sort is not stable in its usual array form: swapping a distant minimum into place jumps over equal keys and reverses them."),
        ],
    },

    "algo_insertionsort": {
        "title": "Insertion Sort",
        "blocks": [
            ("p", "Grow a sorted prefix one element at a time, sliding each new element back to its place."),
            ("code", ("c", "for (i = 1; i < n; i++) {\n    key = a[i];\n    j = i - 1;\n    while (j >= 0 && a[j] > key) { a[j+1] = a[j]; j--; }\n    a[j+1] = key;\n}")),
            ("complexity", ("Time complexity", "O(n) best (already sorted), O(n²) average and worst")),
            ("key", "The running time is Θ(n + I) where I is the number of inversions, so insertion sort is genuinely fast on nearly sorted input. That is why library sorts fall back to it for small or almost-ordered runs."),
            ("note", "Stable, in place, and online: it can sort a stream as elements arrive, which none of the O(n log n) sorts can."),
        ],
    },

    "algo_mergesort": {
        "title": "Merge Sort",
        "blocks": [
            ("p", "Split the array in half, sort each half recursively, then merge the two sorted halves in linear time."),
            ("formula", "T(n) = 2T(n/2) + Θ(n) ⟹ T(n) = Θ(n log n)"),
            ("complexity", ("Time complexity", "Θ(n log n) in all cases")),
            ("complexity", ("Space complexity", "O(n) for the merge buffer")),
            ("key", "The guarantee is the point: unlike quick sort there is no bad input. The price is the extra array, which is why quick sort is usually preferred in memory-tight code."),
            ("note", "Stable if the merge takes from the left half when the two fronts are equal. Reversing that tie-break silently breaks stability."),
            ("example", "Merging [1, 4, 7] and [2, 3, 9] compares 1-2, 4-2, 4-3, 4-9, 7-9 and copies the rest: at most n−1 comparisons for n elements."),
        ],
    },

    "algo_quicksort": {
        "title": "Quick Sort",
        "blocks": [
            ("p", "Choose a pivot, partition the array so smaller elements sit left and larger sit right, then recurse on both sides. The pivot lands in its final position after partitioning."),
            ("table", [
                ["Case", "Partition", "Recurrence", "Time"],
                ["Best", "even split", "T(n) = 2T(n/2) + Θ(n)", "Θ(n log n)"],
                ["Average", "random pivot", "—", "Θ(n log n)"],
                ["Worst", "1 and n−1", "T(n) = T(n−1) + Θ(n)", "Θ(n²)"],
            ]),
            ("warn", "The worst case appears on already sorted input when the first or last element is the pivot — exactly the input people test with. Randomised or median-of-three pivots avoid it."),
            ("complexity", ("Space complexity", "O(log n) average for the recursion stack, O(n) worst")),
            ("key", "Quick sort is not stable and is usually faster than merge sort in practice, because partitioning is a cache-friendly in-place scan."),
        ],
    },

    "algo_heapsort": {
        "title": "Heap Sort",
        "blocks": [
            ("p", "Build a max-heap from the array, then repeatedly swap the root to the end and sift down over the shrinking heap."),
            ("steps", [
                "Build the heap bottom-up in O(n).",
                "Swap a[0] with the last unsorted element.",
                "Reduce the heap size by one and heapify the root in O(log n).",
                "Repeat n−1 times.",
            ]),
            ("complexity", ("Time complexity", "Θ(n log n) in all cases")),
            ("complexity", ("Space complexity", "O(1), sorted in place")),
            ("key", "Building the heap is O(n), not O(n log n). Most nodes are near the leaves and sift down only a level or two; the sum Σ n/2^h · h converges to O(n)."),
            ("note", "Not stable. Heap sort is the usual answer when the question demands O(n log n) worst case with O(1) extra space."),
        ],
    },

    "algo_countsort": {
        "title": "Counting Sort",
        "blocks": [
            ("p", "Count how many times each key occurs, turn the counts into running positions, and place each element directly."),
            ("complexity", ("Time complexity", "Θ(n + k) for keys in the range 0…k")),
            ("complexity", ("Space complexity", "Θ(n + k)")),
            ("key", "Linear only while k is O(n). Sorting 1000 values in the range 0…10⁹ by counting would need a billion counters, so the range is the deciding factor, not the count."),
            ("note", "Stable if the placement loop runs the input backwards. That stability is what makes counting sort usable as the inner step of radix sort."),
        ],
    },

    "algo_radixsort": {
        "title": "Radix Sort",
        "blocks": [
            ("p", "Sort by the least significant digit first, then the next, using a stable sort at each pass. After the last digit the array is fully ordered."),
            ("formula", "T(n) = Θ(d · (n + b)) for d digits in base b"),
            ("key", "The inner sort must be stable, otherwise the ordering achieved by earlier digits is destroyed. Counting sort is the usual choice."),
            ("example", "Sorting 32-bit integers in base 2¹⁶ takes 2 passes of counting sort over 65 536 buckets: linear in n for any n worth sorting."),
            ("warn", "Radix sort is not a comparison sort, so the Ω(n log n) bound does not apply to it. It is not universally faster though — d and b hide real constants."),
        ],
    },

    "algo_bucketsort": {
        "title": "Bucket Sort",
        "blocks": [
            ("p", "Scatter the elements into buckets by value, sort each bucket, then concatenate."),
            ("complexity", ("Time complexity", "Θ(n) average when the input is uniformly distributed, O(n²) worst")),
            ("key", "The average case assumes the keys spread evenly. If every element lands in one bucket the algorithm degenerates to whatever sorts that bucket."),
            ("note", "Used for floating point keys in a known range, where the bucket index is a simple multiply-and-floor."),
        ],
    },

    "algo_divide_conq": {
        "title": "Divide and Conquer",
        "blocks": [
            ("p", "Split the problem into independent subproblems, solve them recursively, and combine. Merge sort, quick sort, binary search and Strassen's multiplication all follow this shape."),
            ("h", "The master theorem"),
            ("formula", "T(n) = a·T(n/b) + f(n),  a ≥ 1, b > 1"),
            ("table", [
                ["Case", "Condition", "Result"],
                ["1", "f(n) = O(n^(log_b a − ε))", "T(n) = Θ(n^(log_b a))"],
                ["2", "f(n) = Θ(n^(log_b a))", "T(n) = Θ(n^(log_b a) · log n)"],
                ["3", "f(n) = Ω(n^(log_b a + ε)) and regular", "T(n) = Θ(f(n))"],
            ]),
            ("example", "Merge sort has a = 2, b = 2, so n^(log₂2) = n and f(n) = Θ(n): case 2 gives Θ(n log n)."),
            ("warn", "The master theorem does not cover every recurrence. T(n) = 2T(n/2) + n/log n falls in the gap between cases 1 and 2 and needs the recursion tree instead."),
        ],
    },

    "algo_greedyalg": {
        "title": "Greedy Algorithms",
        "blocks": [
            ("def", "A greedy algorithm takes the locally best choice at each step and never reconsiders it."),
            ("p", "Greedy is correct only when the problem has the greedy-choice property and optimal substructure. Proving that is the hard part; running the algorithm is easy."),
            ("table", [
                ["Problem", "Greedy works?"],
                ["Minimum spanning tree", "yes (Prim, Kruskal)"],
                ["Single-source shortest path, non-negative weights", "yes (Dijkstra)"],
                ["Fractional knapsack", "yes, by value/weight ratio"],
                ["0/1 knapsack", "no — needs dynamic programming"],
                ["Huffman coding", "yes"],
            ]),
            ("warn", "The 0/1 knapsack is the standard trap. Taking the best ratio first can fill the sack with one item and block two better ones, because items cannot be split."),
        ],
    },

    "algo_huffman": {
        "title": "Huffman Coding",
        "blocks": [
            ("p", "Build an optimal prefix code by repeatedly merging the two least frequent symbols into a subtree."),
            ("steps", [
                "Put every symbol in a min-heap keyed by frequency.",
                "Remove the two smallest, make them children of a new node with the summed frequency, and insert it back.",
                "Repeat until one tree remains; left and right edges label 0 and 1.",
            ]),
            ("complexity", ("Time complexity", "O(n log n) with a binary heap")),
            ("key", "The code is a prefix code, so no codeword is a prefix of another and decoding needs no separators. Frequent symbols end up nearer the root and so get shorter codes."),
            ("example", "Frequencies a:5, b:9, c:12, d:13, e:16, f:45 merge as (5+9)=14, (12+13)=25, (14+16)=30, (25+30)=55, (55+45)=100, giving f a 1-bit code and a the longest."),
            ("note", "The tree is not unique — ties can be broken either way — but the total encoded length is."),
        ],
    },

    "algo_spanningtree": {
        "title": "Spanning Trees",
        "blocks": [
            ("def", "A spanning tree of a connected graph with n vertices is an acyclic connected subgraph containing all n vertices and exactly n−1 edges."),
            ("key", "A minimum spanning tree is unique when all edge weights are distinct. Repeated weights can admit several MSTs of the same total weight."),
            ("bullets", [
                "Adding any non-tree edge creates exactly one cycle.",
                "Removing any tree edge splits the tree into exactly two components.",
                "A complete graph on n vertices has n^(n−2) spanning trees (Cayley's formula).",
            ]),
            ("note", "Cut property: the lightest edge crossing any cut belongs to some MST. Both Prim and Kruskal are that property applied in a different order."),
        ],
    },

    "algo_prims": {
        "title": "Prim's Algorithm",
        "blocks": [
            ("p", "Grow one tree from an arbitrary start vertex, repeatedly adding the cheapest edge that leaves the tree."),
            ("complexity", ("Time complexity", "O(V²) with an adjacency matrix, O(E log V) with a binary heap")),
            ("key", "Prim keeps a single connected tree at every step, which is why it suits dense graphs: the matrix version needs no heap at all."),
            ("note", "Works only on connected graphs. On a disconnected graph it finds the MST of the component it started in."),
        ],
    },

    "algo_krushkalminspanningtree": {
        "title": "Kruskal's Algorithm",
        "blocks": [
            ("p", "Sort every edge by weight and add it if it joins two different components, using union-find to test that."),
            ("complexity", ("Time complexity", "O(E log E), dominated by the sort")),
            ("key", "Kruskal builds a forest that only becomes a tree at the last step, so it handles disconnected graphs naturally, producing a minimum spanning forest."),
            ("table", [
                ["", "Prim", "Kruskal"],
                ["Intermediate state", "one tree", "a forest"],
                ["Data structure", "priority queue", "union-find"],
                ["Suits", "dense graphs", "sparse graphs"],
            ]),
        ],
    },

    "algo_dijasthras": {
        "title": "Dijkstra's Algorithm",
        "blocks": [
            ("p", "Single-source shortest paths on a graph with non-negative weights. Repeatedly settle the unvisited vertex with the smallest tentative distance and relax its outgoing edges."),
            ("formula", "if d[u] + w(u,v) < d[v] then d[v] = d[u] + w(u,v)"),
            ("complexity", ("Time complexity", "O(V²) with an array, O((V + E) log V) with a binary heap")),
            ("warn", "Dijkstra fails on negative edges. Once a vertex is settled it is never revisited, so a later negative edge that would have shortened its path is ignored. Use Bellman-Ford instead."),
            ("key", "A settled vertex has its final distance. That claim is exactly what non-negative weights guarantee and what negative weights destroy."),
        ],
    },

    "algo_bellmenford": {
        "title": "Bellman-Ford Algorithm",
        "blocks": [
            ("p", "Relax every edge V−1 times. Any shortest path has at most V−1 edges, so after V−1 rounds all distances are final."),
            ("complexity", ("Time complexity", "O(V · E)")),
            ("key", "A V-th round that still improves some distance proves a negative-weight cycle is reachable, which is the algorithm's second use: detecting them."),
            ("table", [
                ["", "Dijkstra", "Bellman-Ford"],
                ["Negative edges", "no", "yes"],
                ["Negative cycles", "no", "detects them"],
                ["Time", "O((V+E) log V)", "O(V·E)"],
            ]),
            ("note", "Distributed routing protocols use it because each node needs only its neighbours' estimates, not a global view."),
        ],
    },

    "algo_floydwarshall": {
        "title": "Floyd-Warshall Algorithm",
        "blocks": [
            ("p", "All-pairs shortest paths by dynamic programming: allow one more intermediate vertex at each stage."),
            ("formula", "d[i][j] = min(d[i][j], d[i][k] + d[k][j]) for k = 1…n"),
            ("complexity", ("Time complexity", "Θ(V³)")),
            ("complexity", ("Space complexity", "Θ(V²)")),
            ("warn", "The k loop must be outermost. Swapping the loop order breaks the induction on which intermediate vertices are allowed, and quietly gives wrong answers."),
            ("key", "Handles negative edges. A negative value on the diagonal after the run means a negative cycle passes through that vertex."),
        ],
    },

    "algo_dynamicp": {
        "title": "Dynamic Programming",
        "blocks": [
            ("def", "Dynamic programming solves a problem by combining solutions to overlapping subproblems, storing each subproblem's answer so it is computed once."),
            ("p", "Two conditions must hold: optimal substructure, and overlapping subproblems. Without overlap, plain divide and conquer is already enough."),
            ("table", [
                ["", "Memoisation", "Tabulation"],
                ["Direction", "top-down", "bottom-up"],
                ["Computes", "only what is reached", "every subproblem"],
                ["Cost", "recursion stack", "no recursion"],
            ]),
            ("example", "Naive Fibonacci recomputes fib(3) repeatedly and costs Θ(2ⁿ). Storing each value makes it Θ(n) — same recursion, one table."),
            ("key", "Greedy commits to a choice; dynamic programming tries every choice and keeps the best. That is why 0/1 knapsack needs DP and fractional knapsack does not."),
        ],
    },

    "algo_matrixchain": {
        "title": "Matrix Chain Multiplication",
        "blocks": [
            ("p", "Given dimensions of a chain of matrices, find the parenthesisation that minimises scalar multiplications. The product is the same; the cost is not."),
            ("formula", "m[i][j] = min over k of ( m[i][k] + m[k+1][j] + p(i−1)·p(k)·p(j) )"),
            ("complexity", ("Time complexity", "Θ(n³)")),
            ("complexity", ("Space complexity", "Θ(n²)")),
            ("example", "For A(10×100), B(100×5), C(5×50): (AB)C costs 5000 + 2500 = 7500, while A(BC) costs 25 000 + 50 000 = 75 000 — ten times worse for the same result."),
            ("key", "The number of distinct parenthesisations is the Catalan number C(n−1), which is exponential. The Θ(n³) table is the whole point."),
        ],
    },

    "algo_longestcommensub": {
        "title": "Longest Common Subsequence",
        "blocks": [
            ("p", "Find the longest sequence appearing in both strings in order, not necessarily contiguously."),
            ("formula", "L[i][j] = L[i−1][j−1] + 1 if x[i] = y[j], else max(L[i−1][j], L[i][j−1])"),
            ("complexity", ("Time complexity", "Θ(m · n)")),
            ("example", "LCS of \"ABCBDAB\" and \"BDCABA\" has length 4, for instance BCBA."),
            ("warn", "Subsequence is not substring. A substring must be contiguous; a subsequence need only preserve order, which is why the recurrence may skip a character on either side."),
        ],
    },

    "algo_knapsack01": {
        "title": "0/1 Knapsack",
        "blocks": [
            ("p", "Each item is taken whole or left behind. Maximise value within a weight capacity."),
            ("formula", "V[i][w] = max( V[i−1][w], v(i) + V[i−1][w − w(i)] ) when w(i) ≤ w"),
            ("complexity", ("Time complexity", "Θ(n · W)")),
            ("warn", "Θ(n·W) is pseudo-polynomial, not polynomial: W is a value, and writing it takes only log W bits. The problem is NP-hard, which this table does not contradict."),
            ("key", "The greedy value/weight rule solves the fractional version optimally and the 0/1 version wrongly. That contrast is a standard exam question."),
        ],
    },

    "algo_subsetproblemsum": {
        "title": "Subset Sum Problem",
        "blocks": [
            ("p", "Decide whether some subset of a set of positive integers adds up exactly to a target."),
            ("formula", "S[i][t] = S[i−1][t] OR S[i−1][t − a(i)]"),
            ("complexity", ("Time complexity", "Θ(n · T) by dynamic programming")),
            ("key", "NP-complete in general, and the special case of 0/1 knapsack where value equals weight. The DP table is pseudo-polynomial for the same reason."),
            ("note", "The recursion without a table is Θ(2ⁿ): each element is either taken or not, and every branch is explored."),
        ],
    },

    "algo_spacecomplexity": {
        "title": "Space Complexity",
        "blocks": [
            ("def", "Space complexity is the total memory an algorithm needs; auxiliary space is what it needs beyond the input."),
            ("table", [
                ["Algorithm", "Auxiliary space"],
                ["Merge sort", "O(n)"],
                ["Quick sort", "O(log n) average, O(n) worst"],
                ["Heap sort", "O(1)"],
                ["Counting sort", "O(n + k)"],
                ["Recursive DFS", "O(V) for the stack"],
            ]),
            ("key", "Questions usually mean auxiliary space. An in-place sort still occupies the input array, so quoting O(n) for heap sort is the common mistake."),
            ("note", "Recursion is not free. Every unreturned frame occupies stack, which is why the worst-case depth of quick sort matters as much as its time."),
        ],
    },

    "algo_strassen": {
        "title": "Strassen's Matrix Multiplication",
        "blocks": [
            ("p", "Multiply two n×n matrices with 7 recursive multiplications of half-size blocks instead of the obvious 8, at the cost of more additions."),
            ("formula", "T(n) = 7·T(n/2) + Θ(n²) ⟹ T(n) = Θ(n^log₂7) ≈ Θ(n^2.807)"),
            ("key", "The saving comes entirely from turning 8 multiplications into 7. Applying the master theorem to a = 8 gives back the ordinary Θ(n³)."),
            ("warn", "Faster only for large n. The extra additions and poorer numerical stability mean library code uses the classical algorithm below a sizeable threshold."),
        ],
    },
}
