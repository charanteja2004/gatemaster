# -*- coding: utf-8 -*-
"""Programming and Data Structures revision notes."""

NOTES = {
    "ds_introtoc": {
        "title": "Data Structures and Algorithms",
        "blocks": [
            ("def", "A data structure is a way of organising data so that the operations you need are cheap. Which structure is right depends entirely on which operations dominate."),
            ("table", [
                ["Structure", "Access", "Search", "Insert", "Delete"],
                ["Array", "O(1)", "O(n)", "O(n)", "O(n)"],
                ["Sorted array", "O(1)", "O(log n)", "O(n)", "O(n)"],
                ["Linked list", "O(n)", "O(n)", "O(1)*", "O(1)*"],
                ["BST (balanced)", "O(log n)", "O(log n)", "O(log n)", "O(log n)"],
                ["Hash table", "—", "O(1) avg", "O(1) avg", "O(1) avg"],
                ["Heap", "O(1) for min/max", "O(n)", "O(log n)", "O(log n)"],
            ]),
            ("note", "* Linked list insert and delete are O(1) only once you hold the node. Finding it first is O(n), which is where the confusion in exam options usually lives."),
        ],
    },

    "ds_data_types": {
        "title": "Data Types in C",
        "blocks": [
            ("table", [
                ["Type", "Typical size", "Range"],
                ["char", "1 byte", "−128…127 (signed)"],
                ["int", "4 bytes", "−2³¹…2³¹−1"],
                ["float", "4 bytes", "≈ 6 decimal digits"],
                ["double", "8 bytes", "≈ 15 decimal digits"],
            ]),
            ("warn", "Sizes are not fixed by the standard. Only the minimum ranges and sizeof(char) == 1 are guaranteed, which is why portable code uses stdint types."),
            ("key", "Signed overflow is undefined behaviour in C; unsigned arithmetic wraps modulo 2ⁿ and is well defined. That difference is examined more often than the sizes."),
        ],
    },

    "ds_operators": {
        "title": "Operators in C",
        "blocks": [
            ("table", [
                ["Precedence", "Operators", "Associativity"],
                ["highest", "() [] -> .", "left to right"],
                ["", "! ~ ++ -- (type) * &", "right to left"],
                ["", "* / %", "left to right"],
                ["", "+ −", "left to right"],
                ["", "<< >>", "left to right"],
                ["", "&& then ||", "left to right"],
                ["lowest", "= += −= etc, then comma", "right to left"],
            ]),
            ("key", "&& and || short-circuit: the right operand is not evaluated if the left already decides the result. Side effects hidden there simply do not happen."),
            ("warn", "Expressions like i = i++ + ++i are undefined behaviour, not a puzzle with a correct answer. The standard gives no sequencing between the modifications."),
        ],
    },

    "ds_typecasting": {
        "title": "Type Casting in C",
        "blocks": [
            ("p", "Implicit conversion happens along the usual arithmetic conversions; explicit conversion is written with a cast."),
            ("formula", "char → short → int → unsigned → long → float → double"),
            ("example", "5 / 2 is 2 because both operands are int. 5 / 2.0 is 2.5 because the int is promoted to double before the division."),
            ("warn", "Mixing signed and unsigned promotes the signed operand to unsigned. −1 < 1u is false, because −1 becomes a very large unsigned value."),
        ],
    },

    "ds_decision_making": {
        "title": "Decision Making in C",
        "blocks": [
            ("bullets", [
                "if / else if / else for arbitrary conditions.",
                "switch for equality against integer constants only.",
                "The conditional operator ?: as an expression rather than a statement.",
            ]),
            ("warn", "A switch case without break falls through to the next case. Fall-through is occasionally intended and far more often a bug, and exam questions rely on it."),
            ("key", "In C any non-zero value is true, including negative numbers and pointers. Comparing to 1 rather than to non-zero is a common source of wrong answers."),
        ],
    },

    "ds_loops": {
        "title": "Loops",
        "blocks": [
            ("table", [
                ["Loop", "Test", "Runs at least once"],
                ["for", "before each iteration", "no"],
                ["while", "before each iteration", "no"],
                ["do-while", "after each iteration", "yes"],
            ]),
            ("key", "Counting iterations is the exam skill. for (i = 0; i < n; i++) runs n times; for (i = 1; i <= n; i *= 2) runs ⌊log₂ n⌋ + 1 times."),
            ("example", "A nested loop where the inner bound is the outer index runs 0 + 1 + … + (n−1) = n(n−1)/2 times, which is Θ(n²) even though it looks half as costly."),
        ],
    },

    "ds_functions": {
        "title": "Functions",
        "blocks": [
            ("p", "C passes every argument by value. Passing a pointer passes the address by value, which is what lets the callee modify the caller's object."),
            ("code", ("c", "void swap(int *a, int *b) {\n    int t = *a; *a = *b; *b = t;\n}\nswap(&x, &y);          /* works */\n\nvoid bad(int a, int b) { int t = a; a = b; b = t; }\nbad(x, y);             /* does nothing */")),
            ("key", "An array argument decays to a pointer to its first element, so sizeof inside the function gives the pointer size, not the array size."),
            ("note", "Activation records are pushed on the stack per call, which is why deep recursion overflows it."),
        ],
    },

    "ds_recursion": {
        "title": "Recursion",
        "blocks": [
            ("def", "A recursive function solves a problem by calling itself on a smaller instance, with a base case that stops the descent."),
            ("p", "Each call gets its own activation record holding parameters, locals and the return address. Space is therefore proportional to the maximum depth."),
            ("example", "Towers of Hanoi: T(n) = 2T(n−1) + 1, giving 2ⁿ − 1 moves for n disks."),
            ("key", "Tail recursion, where the recursive call is the last action, can be turned into a loop with no stack growth. C compilers may do this; the standard does not require it."),
            ("warn", "A missing or unreachable base case is not an infinite loop but a stack overflow: each call consumes memory that is never released."),
        ],
    },

    "ds_fibonachi": {
        "title": "Fibonacci Series",
        "blocks": [
            ("formula", "F(0) = 0, F(1) = 1, F(n) = F(n−1) + F(n−2)"),
            ("table", [
                ["Method", "Time", "Space"],
                ["Naive recursion", "Θ(φⁿ) ≈ Θ(1.618ⁿ)", "O(n)"],
                ["Memoised", "Θ(n)", "O(n)"],
                ["Iterative, two variables", "Θ(n)", "O(1)"],
                ["Matrix power", "Θ(log n)", "O(1)"],
            ]),
            ("key", "The naive version recomputes F(n−2) twice, F(n−3) three times and so on — the call tree has about F(n) leaves. It is the standard motivation for dynamic programming."),
        ],
    },

    "ds_pointers": {
        "title": "Pointers",
        "blocks": [
            ("def", "A pointer holds the address of an object. Dereferencing with * reads or writes the object at that address."),
            ("formula", "p + i advances by i × sizeof(*p) bytes, not by i bytes"),
            ("example", "For int *p pointing at address 1000 on a 4-byte-int machine, p + 3 is address 1012."),
            ("bullets", [
                "int *p — pointer to int.",
                "int **p — pointer to pointer.",
                "int (*p)[10] — pointer to an array of 10 ints.",
                "int *p[10] — array of 10 pointers to int.",
            ]),
            ("warn", "Read declarations from the identifier outwards, following precedence: [] and () bind tighter than *. That rule decides the last two lines above."),
            ("key", "A dangling pointer refers to freed memory; a memory leak is allocated memory with no pointer left to it. Both are undefined behaviour territory and both are examined."),
        ],
    },

    "ds_array": {
        "title": "Arrays",
        "blocks": [
            ("p", "A contiguous block of equal-sized elements, so the address of any element is a constant-time calculation."),
            ("formula", "Address of a[i] = base + i × size"),
            ("formula", "Row major: a[i][j] = base + ((i × cols) + j) × size"),
            ("formula", "Column major: a[i][j] = base + ((j × rows) + i) × size"),
            ("example", "For int a[5][4] at base 2000 with 4-byte ints, a[2][3] in row-major order sits at 2000 + (2×4 + 3)×4 = 2044."),
            ("key", "C stores arrays in row-major order. Questions that specify column-major are usually about Fortran or are testing whether you noticed."),
        ],
    },

    "ds_strings": {
        "title": "Strings",
        "blocks": [
            ("p", "A C string is a char array terminated by '\\0'. The terminator is what every library function looks for, and it occupies a byte."),
            ("example", "char s[] = \"GATE\" occupies 5 bytes; strlen(s) is 4 and sizeof(s) is 5."),
            ("table", [
                ["Function", "Does", "Cost"],
                ["strlen", "counts to the terminator", "O(n)"],
                ["strcpy", "copies including terminator", "O(n)"],
                ["strcmp", "lexicographic comparison", "O(n)"],
                ["strcat", "appends, rescanning the target", "O(n + m)"],
            ]),
            ("warn", "strcpy and strcat do not check the destination size. Writing past the end is the classic buffer overflow, and it is undefined behaviour rather than a runtime error."),
        ],
    },

    "ds_struct": {
        "title": "Structures, Unions and Enumerations",
        "blocks": [
            ("table", [
                ["", "struct", "union"],
                ["Members", "each has its own storage", "all share one region"],
                ["Size", "sum of members plus padding", "size of the largest member"],
                ["Valid at once", "all", "one"],
            ]),
            ("key", "Padding makes sizeof(struct) more than the sum of its members: each member is aligned to its own size, and the whole struct is padded to the largest alignment."),
            ("example", "struct { char c; int i; } is usually 8 bytes, not 5 — three padding bytes sit after c so that i starts on a 4-byte boundary."),
            ("note", "Reordering members from largest to smallest often shrinks the structure, which is a standard exam calculation as well as a real optimisation."),
        ],
    },

    "ds_ll": {
        "title": "Linked Lists",
        "blocks": [
            ("def", "A linked list stores each element in a node holding the data and a pointer to the next node, so elements need not be contiguous."),
            ("table", [
                ["", "Array", "Linked list"],
                ["Access by index", "O(1)", "O(n)"],
                ["Insert at front", "O(n)", "O(1)"],
                ["Memory", "contiguous, may need resizing", "per-node pointer overhead"],
                ["Cache behaviour", "excellent", "poor"],
            ]),
            ("key", "Insertion is O(1) given the previous node. Every list question turns on whether you already hold that pointer or must search for it."),
            ("note", "Floyd's cycle detection — a slow and a fast pointer — finds a loop in O(n) time and O(1) space and is the most examined list algorithm."),
        ],
    },

    "ds_sll": {
        "title": "Singly Linked List",
        "blocks": [
            ("p", "One pointer per node, traversal in one direction only."),
            ("code", ("c", "struct node { int data; struct node *next; };\n\n/* insert at front */\nn->next = head;\nhead = n;\n\n/* delete after p */\ntmp = p->next;\np->next = tmp->next;\nfree(tmp);")),
            ("key", "Deleting a node needs its predecessor, which a singly linked list cannot reach from the node itself. That single limitation is the reason doubly linked lists exist."),
            ("warn", "Reversing a list needs three pointers — previous, current and next — because overwriting current->next loses the rest of the list."),
        ],
    },

    "ds_dll": {
        "title": "Doubly Linked List",
        "blocks": [
            ("p", "Each node carries pointers both ways, so traversal and deletion work from either direction."),
            ("table", [
                ["", "Singly", "Doubly"],
                ["Pointers per node", "1", "2"],
                ["Delete a known node", "O(n) — needs predecessor", "O(1)"],
                ["Backward traversal", "no", "yes"],
            ]),
            ("key", "The cost is one extra pointer per node and two pointer updates per operation instead of one. LRU caches use a doubly linked list precisely for the O(1) removal."),
        ],
    },

    "ds_stack": {
        "title": "Stack",
        "blocks": [
            ("def", "A stack is a last-in-first-out collection with push, pop and top, all O(1)."),
            ("bullets", [
                "Function call management and recursion.",
                "Expression conversion and evaluation.",
                "Undo, backtracking, and iterative DFS.",
                "Balanced bracket checking.",
            ]),
            ("key", "For n distinct items pushed in order, the number of valid push-pop sequences is the Catalan number C(n) = (2n)! / (n!·(n+1)!). C(3) = 5 and C(4) = 14 appear regularly."),
            ("note", "Implemented on an array, push and pop are O(1) until a resize is needed; on a linked list they are always O(1) with a pointer per node."),
        ],
    },

    "ds_infixtopostfix": {
        "title": "Infix to Postfix Conversion",
        "blocks": [
            ("p", "Postfix needs no brackets and no precedence rules, which is why machines evaluate it."),
            ("steps", [
                "Operands go straight to the output.",
                "An operator pops operators of greater or equal precedence, then is pushed.",
                "( is pushed; ) pops until the matching ( is removed.",
                "At the end, pop everything left.",
            ]),
            ("example", "A + B * C becomes A B C * +.  (A + B) * C becomes A B + C *."),
            ("key", "Right-associative operators such as ^ pop only strictly greater precedence, otherwise A ^ B ^ C would associate the wrong way."),
            ("note", "Evaluating postfix is a single stack pass: push operands, and on an operator pop two, apply, push the result."),
        ],
    },

    "ds_queue": {
        "title": "Queue",
        "blocks": [
            ("def", "A first-in-first-out collection with enqueue at the rear and dequeue at the front, both O(1)."),
            ("bullets", [
                "CPU and disk scheduling.",
                "Breadth-first search.",
                "Buffers between a producer and a consumer.",
            ]),
            ("warn", "A naive array queue drifts: after enough operations, front and rear reach the end of the array while free space sits unused at the start. The circular queue fixes exactly this."),
        ],
    },

    "ds_circularqueue": {
        "title": "Circular Queue",
        "blocks": [
            ("p", "Wrap the indices with modulo arithmetic so the free space at the front is reusable."),
            ("formula", "rear = (rear + 1) % size;   front = (front + 1) % size"),
            ("key", "Full and empty both give front == rear. Either keep a count, or leave one slot unused and call it full when (rear + 1) % size == front — which is why a size-n circular queue often holds n−1 elements."),
        ],
    },

    "ds_priorityqueue": {
        "title": "Priority Queue",
        "blocks": [
            ("def", "A collection where the element removed is always the one with the highest priority, not the one that arrived first."),
            ("table", [
                ["Implementation", "Insert", "Extract best"],
                ["Unsorted array", "O(1)", "O(n)"],
                ["Sorted array", "O(n)", "O(1)"],
                ["Binary heap", "O(log n)", "O(log n)"],
            ]),
            ("key", "The heap is the usual answer because it balances both operations. Dijkstra, Prim and Huffman are all priority queues in a loop."),
        ],
    },

    "ds_trees": {
        "title": "Trees",
        "blocks": [
            ("def", "A tree is a connected acyclic graph. A rooted tree with n nodes has exactly n−1 edges and one path between any two nodes."),
            ("bullets", [
                "A binary tree of height h has at most 2^(h+1) − 1 nodes.",
                "A complete binary tree with n nodes has height ⌊log₂ n⌋.",
                "In a binary tree, nodes with two children = leaves − 1.",
            ]),
            ("h", "Traversals"),
            ("table", [
                ["Traversal", "Order", "Yields for a BST"],
                ["Inorder", "left, root, right", "sorted sequence"],
                ["Preorder", "root, left, right", "useful for copying"],
                ["Postorder", "left, right, root", "useful for deletion"],
                ["Level order", "breadth first", "needs a queue"],
            ]),
            ("key", "Inorder alone does not determine a tree. Inorder plus preorder, or inorder plus postorder, does — which is why reconstruction questions always supply inorder."),
            ("note", "In a BST, search, insert and delete are O(h). That is O(log n) only while the tree stays balanced; inserting sorted data makes it a linked list with O(n) operations."),
        ],
    },

    "ds_heap": {
        "title": "Heaps",
        "blocks": [
            ("def", "A binary heap is a complete binary tree where every parent dominates its children — greater for a max-heap, smaller for a min-heap."),
            ("formula", "For index i (0-based): left = 2i+1, right = 2i+2, parent = ⌊(i−1)/2⌋"),
            ("table", [
                ["Operation", "Cost"],
                ["Find min or max", "O(1)"],
                ["Insert", "O(log n)"],
                ["Extract root", "O(log n)"],
                ["Build from an array", "O(n)"],
            ]),
            ("key", "Because the tree is complete it lives in an array with no pointers, and its height is always ⌊log₂ n⌋. A heap is not a search structure: finding an arbitrary value is still O(n)."),
        ],
    },

    "ds_graphs": {
        "title": "Graphs",
        "blocks": [
            ("def", "A graph G = (V, E) is a set of vertices and a set of edges between them, directed or not, weighted or not."),
            ("table", [
                ["", "Adjacency matrix", "Adjacency list"],
                ["Space", "Θ(V²)", "Θ(V + E)"],
                ["Edge exists?", "O(1)", "O(degree)"],
                ["Iterate neighbours", "O(V)", "O(degree)"],
                ["Suits", "dense graphs", "sparse graphs"],
            ]),
            ("bullets", [
                "A simple undirected graph on n vertices has at most n(n−1)/2 edges.",
                "The sum of degrees is 2|E|.",
                "A connected graph needs at least n−1 edges; more than that guarantees a cycle.",
            ]),
        ],
    },

    "ds_bfs": {
        "title": "Breadth-First Search",
        "blocks": [
            ("p", "Explore all neighbours of a vertex before moving further out, using a queue."),
            ("complexity", ("Time complexity", "O(V + E) with an adjacency list")),
            ("key", "BFS finds the shortest path in an unweighted graph, because it reaches every vertex by the fewest edges first. With weights, that guarantee disappears and Dijkstra is needed."),
            ("note", "The BFS tree's level of a vertex is its distance in edges from the source. Cross edges in an undirected BFS connect vertices whose levels differ by at most one."),
        ],
    },

    "ds_dfs": {
        "title": "Depth-First Search",
        "blocks": [
            ("p", "Follow one path as deep as possible, then backtrack. Recursive by nature, or explicit with a stack."),
            ("complexity", ("Time complexity", "O(V + E) with an adjacency list")),
            ("bullets", [
                "Cycle detection: a back edge to a vertex still on the recursion stack.",
                "Topological sort: reverse of the finishing order in a DAG.",
                "Strongly connected components: Kosaraju's two passes, or Tarjan's one.",
            ]),
            ("key", "In a directed graph, a cycle exists exactly when DFS finds a back edge. Reaching an already-visited vertex is not enough — it must still be on the current stack."),
        ],
    },

    "ds_variables": {
        "title": "Variables and Storage Classes in C",
        "blocks": [
            ("table", [
                ["Class", "Stored in", "Lifetime", "Scope"],
                ["auto", "stack", "the block", "the block"],
                ["static (local)", "data segment", "whole program", "the block"],
                ["static (global)", "data segment", "whole program", "the file"],
                ["extern", "data segment", "whole program", "all files"],
                ["register", "a register if possible", "the block", "the block"],
            ]),
            ("key", "A static local keeps its value between calls but is still invisible outside the function. That combination of lifetime and scope is what questions test."),
            ("note", "Uninitialised globals and statics are zero-initialised; uninitialised locals hold whatever was on the stack, and reading them is undefined behaviour."),
        ],
    },
}
