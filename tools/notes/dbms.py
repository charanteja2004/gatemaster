# -*- coding: utf-8 -*-
"""Database Management Systems revision notes."""

NOTES = {
    "dbms_introtodbms": {
        "title": "Introduction to DBMS",
        "blocks": [
            ("def", "A database management system is software that stores data and controls access to it, providing querying, concurrency, recovery and integrity in one place."),
            ("table", [
                ["", "File system", "DBMS"],
                ["Redundancy", "high, per application", "controlled"],
                ["Consistency", "the application's problem", "enforced by constraints"],
                ["Concurrent access", "unmanaged", "transactions and locking"],
                ["Recovery after a crash", "manual", "logs and rollback"],
            ]),
            ("key", "The point is not storage — files store data too. It is that integrity, concurrency and recovery are solved once, centrally, rather than in every program that touches the data."),
        ],
    },

    "dbms_independence": {
        "title": "Data Abstraction and Independence",
        "blocks": [
            ("table", [
                ["Level", "Describes", "Seen by"],
                ["Physical", "how bytes are stored and indexed", "the DBMS"],
                ["Logical", "tables, columns and relationships", "the DBA"],
                ["View", "the slice one application sees", "the user"],
            ]),
            ("def", "Physical data independence is the ability to change the storage layout without touching the logical schema. Logical data independence is changing the logical schema without touching the views."),
            ("key", "Physical independence is easy and universally achieved; logical independence is hard, because applications are written against the logical schema and notice when it changes."),
        ],
    },

    "dbms_12tire": {
        "title": "One-Tier and Two-Tier Architecture",
        "blocks": [
            ("bullets", [
                "One-tier: the database and the user sit on the same machine. Useful for development, not for sharing.",
                "Two-tier: a client application talks directly to the database server over a protocol such as ODBC or JDBC.",
            ]),
            ("warn", "Two-tier does not scale: each client holds its own connection, and business rules live in every client, so a rule change means redeploying all of them."),
        ],
    },

    "dbms_3tire": {
        "title": "Three-Tier Architecture",
        "blocks": [
            ("p", "A presentation tier, an application tier holding business logic, and a data tier. Clients never speak to the database directly."),
            ("bullets", [
                "Scales, because the middle tier can be replicated and can pool connections.",
                "Secures, because the database is not exposed to clients.",
                "Maintains, because a rule changes in one place.",
            ]),
            ("key", "The middle tier is what makes connection pooling possible, and pooling is what lets a few hundred database connections serve millions of users."),
        ],
    },

    "dbms_introtoer": {
        "title": "The ER Model",
        "blocks": [
            ("def", "An entity-relationship diagram models the world as entity sets, the relationships between them, and the attributes of both."),
            ("table", [
                ["Symbol", "Means"],
                ["Rectangle", "entity set"],
                ["Diamond", "relationship set"],
                ["Ellipse", "attribute"],
                ["Double ellipse", "multivalued attribute"],
                ["Dashed ellipse", "derived attribute"],
                ["Double rectangle", "weak entity"],
                ["Underline", "primary key"],
            ]),
            ("key", "A weak entity has no key of its own and is identified by its owner plus a discriminator. It always participates totally in the identifying relationship."),
            ("h", "Turning it into tables"),
            ("bullets", [
                "1:1 — merge, or put the key of either side into the other.",
                "1:N — put the key of the 1 side into the N side; no separate table.",
                "M:N — always a separate table with both keys.",
            ]),
        ],
    },

    "dbms_recursiverelationship": {
        "title": "Recursive Relationships",
        "blocks": [
            ("def", "A recursive (unary) relationship connects an entity set to itself, so one entity plays two roles in the same relationship."),
            ("example", "An Employee 'manages' another Employee. The table needs a manager_id column that is a foreign key back to the same table's primary key."),
            ("key", "Role names are compulsory in the diagram, otherwise the two ends are indistinguishable. In SQL the same table appears twice in the query with two aliases."),
        ],
    },

    "dbms_miner": {
        "title": "Minimisation of ER Diagrams",
        "blocks": [
            ("p", "Fewer tables mean fewer joins. The rules for collapsing relationships into existing tables are the practical part of ER design."),
            ("bullets", [
                "A 1:N relationship never needs its own table.",
                "A 1:1 relationship with total participation on one side can be merged into that side.",
                "M:N and n-ary relationships always need their own table.",
                "A multivalued attribute always becomes its own table.",
            ]),
            ("key", "The minimum number of tables for a set of entities is the standard exam calculation. Count entities, then add one per M:N relationship and one per multivalued attribute."),
        ],
    },

    "dbms_keys": {
        "title": "Keys in the Relational Model",
        "blocks": [
            ("table", [
                ["Key", "Means"],
                ["Super key", "any attribute set that uniquely identifies a row"],
                ["Candidate key", "a minimal super key"],
                ["Primary key", "the chosen candidate key"],
                ["Alternate key", "the candidate keys not chosen"],
                ["Foreign key", "references a key in another relation"],
                ["Composite key", "a key made of more than one attribute"],
            ]),
            ("key", "Every candidate key is a super key; the reverse fails because super keys need not be minimal. Adding any attribute to a candidate key gives another super key."),
            ("warn", "A primary key may not be NULL. A foreign key may, and a NULL foreign key means 'no related row', not a violation."),
        ],
    },

    "dbms_relationalalgebra": {
        "title": "Relational Algebra",
        "blocks": [
            ("table", [
                ["Operation", "Symbol", "Does"],
                ["Select", "σ", "picks rows matching a condition"],
                ["Project", "π", "picks columns, removing duplicates"],
                ["Union", "∪", "rows in either, both union-compatible"],
                ["Set difference", "−", "rows in the first but not the second"],
                ["Cartesian product", "×", "every pairing"],
                ["Rename", "ρ", "renames a relation or attributes"],
            ]),
            ("key", "Those six are the primitives; join, intersection and division are derived. A natural join is a product, a selection on the common attributes, and a projection."),
            ("warn", "Projection removes duplicates because a relation is a set. SQL's SELECT keeps them unless you write DISTINCT — the most common mismatch between the theory and the language."),
        ],
    },

    "dbms_sql": {
        "title": "Structured Query Language",
        "blocks": [
            ("p", "SQL is declarative: you describe the result you want, and the optimiser decides how to compute it."),
            ("code", ("sql", "SELECT dept, COUNT(*) AS n, AVG(salary) AS avg_pay\nFROM   employee\nWHERE  salary > 30000\nGROUP  BY dept\nHAVING COUNT(*) > 5\nORDER  BY avg_pay DESC;")),
            ("key", "Evaluation order is FROM, WHERE, GROUP BY, HAVING, SELECT, ORDER BY. That is why a column alias defined in SELECT cannot be used in WHERE but can be used in ORDER BY."),
            ("warn", "WHERE filters rows before grouping; HAVING filters groups after. An aggregate in WHERE is an error."),
        ],
    },

    "dbms_typesofsql": {
        "title": "SQL Command Categories",
        "blocks": [
            ("table", [
                ["Category", "Commands", "About"],
                ["DDL", "CREATE, ALTER, DROP, TRUNCATE", "schema"],
                ["DML", "INSERT, UPDATE, DELETE", "rows"],
                ["DQL", "SELECT", "reading"],
                ["DCL", "GRANT, REVOKE", "permissions"],
                ["TCL", "COMMIT, ROLLBACK, SAVEPOINT", "transactions"],
            ]),
            ("key", "DELETE is DML and can be rolled back; TRUNCATE is DDL, commits implicitly and cannot. DROP removes the table itself. That three-way distinction is examined constantly."),
            ("h", "DELETE, TRUNCATE and DROP"),
            ("table", [
                ["", "DELETE", "TRUNCATE", "DROP"],
                ["Category", "DML", "DDL", "DDL"],
                ["Removes", "chosen rows", "all rows", "the table"],
                ["WHERE clause", "yes", "no", "no"],
                ["Rollback", "possible", "no", "no"],
                ["Fires triggers", "yes", "no", "no"],
                ["Resets identity counter", "no", "yes", "n/a"],
            ]),
            ("note", "TRUNCATE is fast because it deallocates whole data pages instead of logging each row. That is also exactly why it cannot be rolled back or fire row triggers."),
            ("h", "Where the categories matter"),
            ("bullets", [
                "DDL statements commit implicitly, so an open transaction ends the moment one runs.",
                "DCL controls who may run what: GRANT SELECT ON employee TO analyst.",
                "TCL bounds the unit of work: everything between two COMMITs succeeds or fails together.",
            ]),
            ("example", "ALTER TABLE employee ADD COLUMN grade INT is DDL; UPDATE employee SET grade = 1 is DML; both change the table, and only the second can be rolled back."),
        ],
    },

    "dbms_where": {
        "title": "Filtering Rows in SQL",
        "blocks": [
            ("p", "WHERE restricts which rows reach the rest of the query."),
            ("table", [
                ["Operator", "Matches"],
                ["BETWEEN a AND b", "inclusive range"],
                ["IN (…)", "any value in the list"],
                ["LIKE 'A%'", "pattern: % any run, _ one character"],
                ["IS NULL", "the only correct NULL test"],
            ]),
            ("warn", "NULL is unknown, not a value. x = NULL is never true, not even when x is NULL — always use IS NULL. Likewise NOT IN with a NULL in the list returns no rows at all."),
            ("key", "Three-valued logic: TRUE, FALSE and UNKNOWN. WHERE keeps only rows where the condition is TRUE, so UNKNOWN behaves like FALSE for filtering but not for NOT."),
        ],
    },

    "dbms_joins": {
        "title": "SQL Joins",
        "blocks": [
            ("table", [
                ["Join", "Keeps"],
                ["INNER", "rows matching on both sides"],
                ["LEFT OUTER", "all left rows, NULLs where no match"],
                ["RIGHT OUTER", "all right rows, NULLs where no match"],
                ["FULL OUTER", "all rows from both sides"],
                ["CROSS", "every pairing, no condition"],
            ]),
            ("formula", "|inner join| ≤ |A| × |B|;  |cross join| = |A| × |B|"),
            ("key", "A natural join matches on every column with the same name in both tables and keeps one copy of each. That implicitness makes it fragile: adding a column with a shared name silently changes the result."),
            ("example", "With A of 10 rows and B of 5, a cross join gives 50 rows; an inner join on a foreign key gives at most 10 if the key is unique in B."),
        ],
    },

    "dbms_objects": {
        "title": "Database Objects",
        "blocks": [
            ("table", [
                ["Object", "Is"],
                ["Table", "stored rows"],
                ["View", "a stored query, computed on use"],
                ["Index", "a structure making lookups fast"],
                ["Trigger", "code run on an event"],
                ["Sequence", "a generator of unique numbers"],
            ]),
            ("key", "A view stores no data. A materialised view does, and therefore needs refreshing — the trade-off between staleness and query cost."),
            ("note", "An index speeds reads and slows writes, because every insert, update and delete must maintain it too."),
        ],
    },

    "dbms_functionaldependency": {
        "title": "Functional Dependency and Attribute Closure",
        "blocks": [
            ("def", "X → Y means any two rows agreeing on X must agree on Y. It is a constraint on every legal instance, not an observation about one."),
            ("h", "Armstrong's axioms"),
            ("bullets", [
                "Reflexivity: if Y ⊆ X then X → Y.",
                "Augmentation: if X → Y then XZ → YZ.",
                "Transitivity: if X → Y and Y → Z then X → Z.",
            ]),
            ("h", "Attribute closure"),
            ("p", "X⁺ is everything determined by X. Start with X, and repeatedly add the right side of any dependency whose left side is already inside."),
            ("key", "X is a candidate key exactly when X⁺ is all attributes and no proper subset of X has that property. Every key question reduces to computing closures."),
            ("example", "R(A,B,C,D) with A→B, B→C, C→D gives A⁺ = ABCD, so A is a candidate key."),
        ],
    },

    "dbms_normalization": {
        "title": "Normalisation",
        "blocks": [
            ("p", "Decompose relations to remove redundancy, so that a fact is stored once and cannot become inconsistent."),
            ("bullets", [
                "Lossless join: the decomposition can be joined back exactly. Non-negotiable.",
                "Dependency preservation: every original dependency is still enforceable on one table. Desirable.",
            ]),
            ("key", "A binary decomposition of R into R1 and R2 is lossless exactly when R1 ∩ R2 determines R1 or determines R2. That test appears in nearly every normalisation question."),
            ("warn", "Normalising past 3NF can cost dependency preservation. BCNF is always achievable losslessly, but not always with dependencies preserved."),
        ],
    },

    "dbms_normalforms": {
        "title": "Normal Forms",
        "blocks": [
            ("table", [
                ["Form", "Requires"],
                ["1NF", "atomic values, no repeating groups"],
                ["2NF", "1NF and no partial dependency on part of a key"],
                ["3NF", "2NF and no transitive dependency on non-prime attributes"],
                ["BCNF", "every determinant is a super key"],
                ["4NF", "BCNF and no non-trivial multivalued dependency"],
            ]),
            ("key", "3NF allows X → Y when Y is a prime attribute; BCNF does not. That single exception is the whole difference, and the reason BCNF may lose dependencies."),
            ("example", "R(A,B,C) with AB → C and C → B is in 3NF because B is prime, but not in BCNF because C is not a super key."),
            ("note", "2NF only matters when the key is composite. With a single-attribute key, 1NF implies 2NF."),
        ],
    },

    "dbms_anomali": {
        "title": "Anomalies in the Relational Model",
        "blocks": [
            ("table", [
                ["Anomaly", "Happens when"],
                ["Insertion", "a fact cannot be recorded without another unrelated fact"],
                ["Deletion", "removing a row loses an unrelated fact"],
                ["Update", "one fact stored in many rows is changed in only some"],
            ]),
            ("example", "A table holding (student, course, dept_head) cannot record a new department's head until a student enrols in it, and loses the head when the last student leaves."),
            ("key", "All three anomalies come from a single cause: a relation representing more than one fact type. Normalisation is the systematic separation of them."),
        ],
    },

    "dbms_acid": {
        "title": "ACID Properties",
        "blocks": [
            ("table", [
                ["Property", "Guarantees"],
                ["Atomicity", "all of the transaction or none of it"],
                ["Consistency", "constraints hold before and after"],
                ["Isolation", "concurrent transactions do not see each other's partial work"],
                ["Durability", "a committed change survives a crash"],
            ]),
            ("key", "Atomicity is delivered by the undo log, durability by the redo log and forced writes at commit, and isolation by locking or timestamps."),
            ("h", "Isolation levels"),
            ("table", [
                ["Level", "Dirty read", "Non-repeatable read", "Phantom"],
                ["Read uncommitted", "possible", "possible", "possible"],
                ["Read committed", "no", "possible", "possible"],
                ["Repeatable read", "no", "no", "possible"],
                ["Serialisable", "no", "no", "no"],
            ]),
            ("note", "A schedule is conflict serialisable when its precedence graph is acyclic. Two-phase locking guarantees it; strict 2PL additionally prevents cascading rollbacks."),
        ],
    },

    "dbms_recoverytech": {
        "title": "Database Recovery",
        "blocks": [
            ("p", "Recovery restores the last consistent state after a crash, using a log written before the data pages."),
            ("key", "Write-ahead logging: the log record reaches stable storage before the change it describes. Without that rule, a crash could leave a change with no record of how to undo it."),
            ("bullets", [
                "Undo: roll back transactions that had not committed.",
                "Redo: reapply transactions that had committed but whose pages were not yet written.",
                "Checkpoints bound how far back recovery must read.",
            ]),
            ("note", "Deferred update writes nothing until commit and so never needs undo. Immediate update writes as it goes and needs both."),
        ],
    },

    "dbms_deadlockindbms": {
        "title": "Deadlock in Databases",
        "blocks": [
            ("p", "Two transactions each hold a lock the other needs. The same circular wait as in operating systems, resolved by aborting one of them."),
            ("table", [
                ["Scheme", "Rule", "Preempts?"],
                ["Wait-die", "older waits, younger dies", "non-preemptive"],
                ["Wound-wait", "older wounds younger, younger waits", "preemptive"],
            ]),
            ("key", "Both use timestamps and both guarantee no cycles, because waiting always runs in one direction of age. A restarted transaction keeps its original timestamp, which is what prevents starvation."),
            ("note", "Detection uses a wait-for graph: a cycle means deadlock, and the victim is usually the transaction with the least work done."),
        ],
    },

    "dbms_starvation": {
        "title": "Starvation",
        "blocks": [
            ("def", "Starvation is a transaction repeatedly delayed or aborted while others proceed, so it never completes despite never being deadlocked."),
            ("bullets", [
                "A stream of shared lock requests can keep an exclusive request waiting forever.",
                "A deadlock resolver that always picks the same victim aborts it every time.",
            ]),
            ("key", "First-come-first-served lock granting, and keeping the original timestamp on restart, are the two standard cures."),
        ],
    },

    "dbms_file": {
        "title": "File Organisation and Indexing",
        "blocks": [
            ("table", [
                ["Organisation", "Search", "Insert"],
                ["Heap", "O(n)", "O(1)"],
                ["Sorted", "O(log n) by binary search", "O(n)"],
                ["Hashed", "O(1) average", "O(1) average"],
                ["B+ tree indexed", "O(log n)", "O(log n)"],
            ]),
            ("def", "A dense index has an entry per record; a sparse index has one per block and needs the file to be sorted on the indexed key."),
            ("key", "Only one clustered index per table, because it dictates the physical order of the rows. Everything else is a secondary index and must be dense."),
            ("h", "B+ trees"),
            ("bullets", [
                "All records live in the leaves, which are chained for range scans.",
                "Internal nodes hold only separator keys, so fan-out is high and the tree is shallow.",
                "Order n means each node holds up to n−1 keys and n pointers.",
            ]),
            ("note", "A B+ tree of order 100 and height 3 indexes about a million records, which is why disk-based indexes are B+ trees rather than binary trees."),
        ],
    },

    "dbms_hashing": {
        "title": "Hashing",
        "blocks": [
            ("p", "A hash function maps a key directly to a bucket, giving constant-time lookup when collisions are rare."),
            ("table", [
                ["Collision handling", "How"],
                ["Chaining", "a list per bucket"],
                ["Linear probing", "try the next slot"],
                ["Quadratic probing", "try i² slots away"],
                ["Double hashing", "step by a second hash"],
            ]),
            ("formula", "Load factor α = number of keys / number of slots"),
            ("key", "Linear probing suffers primary clustering: occupied runs grow and lengthen every later probe. Double hashing spreads the probe sequence and largely avoids it."),
            ("h", "Static against dynamic"),
            ("note", "Static hashing degrades as the file grows past its bucket count. Extendible hashing doubles the directory on overflow, and linear hashing splits one bucket at a time, so both grow without a full rehash."),
        ],
    },
}
