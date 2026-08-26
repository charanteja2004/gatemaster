# -*- coding: utf-8 -*-
"""Operating Systems revision notes."""

NOTES = {
    "os_os_basic1": {
        "title": "What an Operating System Does",
        "blocks": [
            ("def", "An operating system is the layer that manages hardware and gives programs a clean, safe interface to it: processes, memory, files and devices."),
            ("bullets", [
                "Process management: creation, scheduling, synchronisation, termination.",
                "Memory management: allocation, protection, virtual memory.",
                "File and storage management: naming, layout, disk scheduling.",
                "Protection: keeping one process out of another's memory and files.",
            ]),
            ("key", "Two hardware features make it possible at all: dual mode (user and kernel) and a timer interrupt. Without dual mode a program could execute privileged instructions; without the timer it could never be preempted."),
            ("note", "A system call is the controlled doorway between the two modes. It traps into the kernel, which validates the request before touching hardware."),
        ],
    },

    "os_types": {
        "title": "Types of Operating Systems",
        "blocks": [
            ("table", [
                ["Type", "Optimises for", "Example use"],
                ["Batch", "throughput", "payroll runs, no interaction"],
                ["Multiprogrammed", "CPU utilisation", "keeps CPU busy across jobs"],
                ["Time sharing", "response time", "many interactive users"],
                ["Real time", "meeting deadlines", "control systems"],
                ["Distributed", "sharing resources", "clusters"],
            ]),
            ("key", "Multiprogramming keeps several jobs in memory so the CPU switches away on I/O; time sharing adds a timer so it also switches on a quantum. The second is the first plus preemption."),
            ("note", "Hard real time guarantees a deadline is met; soft real time only degrades in value when late. Missing a hard deadline is a system failure, not a slowdown."),
        ],
    },

    "os_structure": {
        "title": "Operating System Structure",
        "blocks": [
            ("table", [
                ["Structure", "Idea", "Trade-off"],
                ["Monolithic", "everything in one kernel address space", "fast, hard to maintain"],
                ["Layered", "each layer uses only the one below", "clean, layer crossings cost"],
                ["Microkernel", "minimal kernel, services in user space", "robust, more message passing"],
                ["Modular", "loadable kernel modules", "monolithic speed, dynamic pieces"],
            ]),
            ("key", "A microkernel keeps only scheduling, memory and IPC in the kernel. A driver crash then kills a user process rather than the machine, at the cost of extra context switches per request."),
            ("note", "Linux is monolithic but modular: modules load into kernel space at run time, so a module bug still takes the kernel down."),
        ],
    },

    "os_systemcalls": {
        "title": "System Calls",
        "blocks": [
            ("def", "A system call is a request from a user process for a service only the kernel may perform. It switches the CPU to kernel mode through a trap."),
            ("table", [
                ["Category", "Examples"],
                ["Process control", "fork, exec, exit, wait"],
                ["File management", "open, read, write, close"],
                ["Device management", "ioctl, read, write"],
                ["Information", "getpid, time"],
                ["Communication", "pipe, shmget, send, recv"],
            ]),
            ("key", "fork returns twice: 0 in the child and the child's pid in the parent. A question asking how many processes a chain of n forks creates is asking for 2ⁿ."),
            ("note", "A system call is not a function call. The mode switch, argument validation and possible scheduling make it orders of magnitude more expensive."),
        ],
    },

    "os_process": {
        "title": "Process",
        "blocks": [
            ("def", "A process is a program in execution, together with its program counter, registers, stack, heap and data section."),
            ("table", [
                ["", "Process", "Thread"],
                ["Address space", "its own", "shared with the process"],
                ["Creation cost", "high", "low"],
                ["Communication", "IPC needed", "shared memory directly"],
                ["Crash impact", "isolated", "kills the whole process"],
            ]),
            ("key", "A program is passive text on disk; a process is the active execution of it. Two processes running the same program are separate processes with separate memory."),
        ],
    },

    "os_pcb": {
        "title": "Process Control Block",
        "blocks": [
            ("def", "The PCB is the kernel's record of a process: everything needed to stop it and later resume it as if nothing happened."),
            ("bullets", [
                "Process id and state.",
                "Program counter and CPU registers.",
                "Scheduling information: priority, queue pointers.",
                "Memory management data: page table or segment table pointers.",
                "Accounting and I/O status, including open files.",
            ]),
            ("key", "A context switch is precisely the act of saving the CPU state into one PCB and loading it from another. The size of that state is what makes switching costly."),
        ],
    },

    "os_processstate": {
        "title": "Process States",
        "blocks": [
            ("table", [
                ["State", "Meaning", "Leaves when"],
                ["New", "being created", "admitted"],
                ["Ready", "waiting for the CPU", "scheduler dispatches it"],
                ["Running", "executing", "preempted, exits, or waits"],
                ["Waiting", "blocked on I/O or an event", "the event completes"],
                ["Terminated", "finished", "—"],
            ]),
            ("key", "Running to Ready is preemption; Running to Waiting is a voluntary block. Waiting never goes straight to Running — a woken process joins the ready queue and waits its turn."),
            ("note", "A zombie has terminated but its parent has not collected the exit status; an orphan has lost its parent and is adopted by init."),
        ],
    },

    "os_contactswitching": {
        "title": "Context Switching",
        "blocks": [
            ("def", "A context switch saves the state of the running process into its PCB and restores another process's state so it can resume."),
            ("p", "It is pure overhead: no user work happens during the switch. That cost sets the floor on how small a scheduling quantum can usefully be."),
            ("key", "Switching between threads of the same process is cheaper than between processes, because the address space and page tables do not change and the TLB need not be flushed."),
            ("warn", "A very small round-robin quantum gives excellent response time and terrible throughput, as the CPU spends its time switching rather than computing."),
        ],
    },

    "os_multitasking": {
        "title": "Multitasking and Multiprogramming",
        "blocks": [
            ("table", [
                ["Term", "Means"],
                ["Multiprogramming", "several jobs in memory, CPU switches when one blocks"],
                ["Multitasking", "multiprogramming plus timer-driven preemption"],
                ["Multiprocessing", "more than one physical CPU"],
                ["Multithreading", "several threads inside one process"],
            ]),
            ("key", "Multiprogramming raises CPU utilisation; multitasking raises responsiveness. They answer different questions and are often confused in exam options."),
            ("formula", "CPU utilisation ≈ 1 − pⁿ  for n processes each spending fraction p of their time waiting on I/O"),
        ],
    },

    "os_cs": {
        "title": "Race Conditions and the Critical Section",
        "blocks": [
            ("def", "A race condition occurs when the result depends on the interleaving of concurrent accesses to shared data."),
            ("p", "The fix is mutual exclusion over the critical section — the code that touches the shared data."),
            ("bullets", [
                "Mutual exclusion: at most one process inside the critical section.",
                "Progress: if nobody is inside, a waiting process must be able to enter.",
                "Bounded waiting: a bound on how many others may enter first.",
            ]),
            ("example", "Two threads both run count++. Each reads 5, adds 1, writes 6, and one increment is lost — because ++ is a read, a modify and a write, not one instruction."),
            ("warn", "A correct solution must satisfy all three conditions. A lock that lets one process starve satisfies mutual exclusion and still fails bounded waiting."),
        ],
    },

    "os_preemptive": {
        "title": "Preemptive and Non-preemptive Scheduling",
        "blocks": [
            ("table", [
                ["", "Non-preemptive", "Preemptive"],
                ["CPU released", "only on exit or block", "also on timer or higher priority"],
                ["Response time", "poor", "good"],
                ["Overhead", "low", "context switches"],
                ["Examples", "FCFS, SJF, LJF", "SRTF, Round Robin, LRTF"],
            ]),
            ("key", "Preemption is what makes starvation possible for long jobs and makes shared data need locking. Both problems are absent from a non-preemptive scheduler and so is responsiveness."),
        ],
    },

    "os_fcfs": {
        "title": "First Come First Serve Scheduling",
        "blocks": [
            ("p", "Run processes in arrival order, to completion. Simple, fair by arrival, and non-preemptive."),
            ("example", "P1, P2, P3 arrive at 0 with bursts 24, 3, 3. Waiting times are 0, 24, 27 and the average is 17. Running the short ones first gives 3."),
            ("key", "The convoy effect: one long job at the head makes every short job behind it wait, which is why FCFS has poor average waiting time."),
            ("formula", "Turnaround = completion − arrival;  Waiting = turnaround − burst"),
            ("note", "FCFS never starves anyone — every process eventually reaches the head of the queue."),
        ],
    },

    "os_sjf": {
        "title": "Shortest Job First",
        "blocks": [
            ("p", "Pick the ready process with the smallest CPU burst. The preemptive form is Shortest Remaining Time First."),
            ("key", "SJF is provably optimal for average waiting time among non-preemptive schedules. No other order of the same jobs does better."),
            ("warn", "It needs the burst length in advance, which is not knowable. Real systems estimate it with an exponential average of past bursts."),
            ("formula", "τ(n+1) = α · t(n) + (1 − α) · τ(n)"),
            ("note", "Long jobs can starve if short jobs keep arriving. Ageing — raising priority with waiting time — is the standard cure."),
        ],
    },

    "os_ljf": {
        "title": "Longest Job First",
        "blocks": [
            ("p", "Non-preemptive scheduling that picks the ready process with the largest CPU burst. It is the mirror image of Shortest Job First, and deliberately a bad idea."),
            ("steps", [
                "At each scheduling point, look at every process that has arrived.",
                "Choose the one with the longest burst; break ties by arrival time.",
                "Run it to completion, then repeat.",
            ]),
            ("example", "Bursts 2, 4, 8 arriving together finish at 8, 12 and 14, so waiting times are 0, 8, 12 and the average is 6.67. SJF on the same jobs averages 2.67."),
            ("key", "Because SJF provably minimises average waiting time, LJF maximises it among non-preemptive orderings of the same jobs. It is the worst case that proves the SJF result meaningful."),
            ("warn", "Short jobs starve while long ones keep arriving, and the convoy effect is worse than under FCFS: here the long job is chosen deliberately rather than by accident of arrival."),
            ("note", "It appears in exams as a Gantt chart exercise and as the contrast that makes SJF's optimality concrete. No production scheduler uses it."),
        ],
    },

    "os_lrtf": {
        "title": "Longest Remaining Time First",
        "blocks": [
            ("p", "The preemptive form of Longest Job First: at every arrival, switch to whichever ready process has the most work left."),
            ("warn", "Produces many context switches and poor averages. It appears in exams as a calculation exercise rather than as a practical scheduler."),
            ("note", "With ties broken by arrival, several processes tend to finish at nearly the same instant, which is the giveaway when checking a Gantt chart."),
        ],
    },

    "os_rr": {
        "title": "Round Robin Scheduling",
        "blocks": [
            ("p", "Each ready process runs for at most one time quantum, then goes to the back of the queue. Preemptive by construction."),
            ("key", "Quantum choice decides the behaviour. Very large quantum degenerates to FCFS; very small quantum spends everything on context switches."),
            ("formula", "For n processes and quantum q, no process waits more than (n − 1) · q"),
            ("example", "Bursts 24, 3, 3 with q = 4: the Gantt chart runs P1, P2, P3, P1, P1… and the average waiting time falls to 5.66 against FCFS's 17."),
            ("note", "Round robin gives the best response time of the classical schedulers and rarely the best average turnaround."),
        ],
    },

    "os_priority": {
        "title": "Priority Scheduling",
        "blocks": [
            ("p", "Each process carries a priority; the scheduler picks the highest. It exists in preemptive and non-preemptive forms."),
            ("warn", "Low-priority processes can starve indefinitely. Ageing raises a waiting process's priority over time and fixes it."),
            ("key", "Priority inversion: a high-priority task blocks on a lock held by a low-priority task, which is itself preempted by a medium-priority task. Priority inheritance — briefly raising the holder's priority — is the standard fix."),
            ("note", "SJF is priority scheduling where the priority is the inverse of the next burst."),
        ],
    },

    "os_hrrn": {
        "title": "Highest Response Ratio Next",
        "blocks": [
            ("p", "Non-preemptive scheduling that balances burst length against how long a process has already waited."),
            ("formula", "Response ratio = (waiting time + burst time) / burst time"),
            ("key", "Short jobs still win, but a long job's ratio grows as it waits, so it eventually overtakes. HRRN gets much of SJF's average while avoiding its starvation."),
            ("example", "A job with burst 10 that has waited 20 has ratio 3.0, beating a fresh job with burst 4 and ratio 1.0."),
        ],
    },

    "os_mq": {
        "title": "Multilevel Queue Scheduling",
        "blocks": [
            ("p", "Split the ready queue into several queues with different policies, for instance interactive jobs on round robin and batch jobs on FCFS."),
            ("key", "In a plain multilevel queue a process never changes queue. Multilevel feedback queues let a process move down when it uses a whole quantum and up when it waits, which approximates SJF without knowing burst lengths."),
            ("note", "Scheduling between the queues is itself a policy: fixed priority risks starving the lower queues, so time slices are often shared out instead."),
        ],
    },

    "os_ipc": {
        "title": "Inter-Process Communication",
        "blocks": [
            ("table", [
                ["Model", "How", "Cost"],
                ["Shared memory", "a region mapped into both", "fast, needs synchronisation"],
                ["Message passing", "send and receive through the kernel", "safe, a copy per message"],
            ]),
            ("key", "Shared memory is fast because the kernel is involved only at setup; every access afterwards is ordinary memory. Message passing crosses into the kernel each time."),
            ("bullets", [
                "Pipes: one-way, between related processes.",
                "Named pipes (FIFOs): survive as file system entries.",
                "Sockets: work across machines.",
            ]),
        ],
    },

    "os_semaphores": {
        "title": "Semaphores",
        "blocks": [
            ("def", "A semaphore is an integer accessed only through two atomic operations: wait (P), which decrements and blocks at zero, and signal (V), which increments and wakes a waiter."),
            ("table", [
                ["Type", "Values", "Used for"],
                ["Binary", "0 or 1", "mutual exclusion"],
                ["Counting", "0…n", "a pool of n identical resources"],
            ]),
            ("key", "A mutex has an owner and only the owner may release it; a binary semaphore has no owner and may be signalled by any thread. That difference matters in exam options."),
            ("warn", "Order matters. Swapping wait(mutex) and wait(full) in the producer-consumer solution produces deadlock, which is the classic examined trap."),
        ],
    },

    "os_pcproblem": {
        "title": "Producer-Consumer Problem",
        "blocks": [
            ("p", "A producer fills a bounded buffer and a consumer empties it. The buffer must never overflow, underflow, or be touched by both at once."),
            ("code", ("c", "// producer                 // consumer\nwait(empty);               wait(full);\nwait(mutex);               wait(mutex);\n  add item to buffer         remove item\nsignal(mutex);             signal(mutex);\nsignal(full);              signal(empty);")),
            ("key", "Three semaphores: mutex for exclusion, empty counting free slots, full counting filled slots. empty starts at n and full at 0."),
            ("warn", "The counting semaphore must be acquired before the mutex. Taking mutex first lets a full-buffer producer sleep while holding the lock, and nothing can ever drain the buffer."),
        ],
    },

    "os_rwproblem": {
        "title": "The Readers-Writers Problem",
        "blocks": [
            ("p", "Any number of readers may share the data, but a writer needs exclusive access."),
            ("table", [
                ["Variant", "Priority", "Starves"],
                ["First", "readers", "writers"],
                ["Second", "writers", "readers"],
                ["Third", "neither, FIFO", "nobody"],
            ]),
            ("key", "The first solution lets a stream of overlapping readers keep a writer waiting forever, because the writer only gets in when the reader count reaches zero."),
            ("note", "A read-write lock in a modern library is this problem solved once, usually in the fair variant."),
        ],
    },

    "os_dpproblem": {
        "title": "The Dining Philosophers Problem",
        "blocks": [
            ("p", "Five philosophers, five forks, each needing both neighbouring forks to eat. The illustration of deadlock and starvation in one picture."),
            ("warn", "If every philosopher picks up the left fork first, all five hold one fork and wait forever: circular wait, and therefore deadlock."),
            ("bullets", [
                "Allow at most four philosophers at the table at once.",
                "Pick up both forks only if both are free, in one atomic step.",
                "Make one philosopher left-handed, breaking the symmetry and the cycle.",
            ]),
            ("key", "Each fix removes one of Coffman's four conditions. The asymmetric solution is the cheapest and the one usually expected."),
        ],
    },

    "os_dead": {
        "title": "Deadlock",
        "blocks": [
            ("def", "A set of processes is deadlocked when every process in the set waits for an event that only another process in the set can cause."),
            ("h", "Coffman's four conditions"),
            ("steps", [
                "Mutual exclusion: at least one resource is non-shareable.",
                "Hold and wait: a process holds one resource while requesting another.",
                "No preemption: a resource is released only voluntarily.",
                "Circular wait: a cycle of processes each waiting on the next.",
            ]),
            ("key", "All four must hold at once. Breaking any single one prevents deadlock, and every prevention scheme is exactly that: an attack on one condition."),
            ("note", "Starvation is not deadlock. A starving process could run if the scheduler chose it; a deadlocked one could not run whatever the scheduler did."),
        ],
    },

    "os_resource": {
        "title": "Resource Allocation Graph",
        "blocks": [
            ("p", "Processes and resources as nodes; a request edge points from a process to a resource, an assignment edge from a resource instance back to a process."),
            ("key", "With one instance per resource type, a cycle means deadlock. With several instances, a cycle is necessary but not sufficient — the cycle may resolve when an instance is freed."),
            ("example", "P1 → R1 → P2 → R2 → P1 with single instances is a deadlock. The same cycle with two instances of R2 may not be."),
        ],
    },

    "os_detection": {
        "title": "Deadlock Detection",
        "blocks": [
            ("p", "Let deadlock happen, then find it by searching for a set of processes whose requests no available resources can satisfy."),
            ("steps", [
                "Start with Work = Available and mark processes with no allocation as finished.",
                "Find an unfinished process whose Request ≤ Work.",
                "Add its allocation to Work and mark it finished.",
                "Repeat; any process left unfinished is deadlocked.",
            ]),
            ("complexity", ("Time complexity", "O(m · n²) for n processes and m resource types")),
            ("note", "Detection has to be paired with recovery: kill a process, or roll one back and preempt its resources. Both lose work."),
        ],
    },

    "os_bankers": {
        "title": "Banker's Algorithm",
        "blocks": [
            ("p", "Deadlock avoidance: before granting a request, check whether the resulting state still has some order in which every process can finish."),
            ("def", "A state is safe if there is a sequence P1…Pn where each Pi's remaining need can be met by the currently available resources plus everything held by the processes before it."),
            ("steps", [
                "Need = Max − Allocation.",
                "Work = Available; Finish[i] = false for all i.",
                "Find i with Finish[i] false and Need(i) ≤ Work.",
                "Work = Work + Allocation(i); Finish[i] = true; repeat.",
                "If every process finishes, the state is safe.",
            ]),
            ("key", "Unsafe is not the same as deadlocked. An unsafe state may still complete if processes ask for less than their declared maximum; the algorithm simply refuses to risk it."),
            ("warn", "Every process must declare its maximum need in advance, which is why the algorithm is rare outside exams."),
        ],
    },

    "os_methods": {
        "title": "Handling Deadlocks",
        "blocks": [
            ("table", [
                ["Approach", "How", "Cost"],
                ["Prevention", "break one Coffman condition structurally", "poor utilisation"],
                ["Avoidance", "grant only requests that keep the state safe", "needs maximum claims"],
                ["Detection and recovery", "let it happen, then fix", "lost work"],
                ["Ostrich algorithm", "ignore it", "free until it bites"],
            ]),
            ("key", "General purpose systems mostly ignore deadlock. It is rare, the prevention cost is constant, and a reboot is cheaper than the accounting."),
            ("note", "Requesting resources in a fixed global order prevents circular wait and is the practical technique used in real code."),
        ],
    },

    "os_memorymanagement": {
        "title": "Memory Management",
        "blocks": [
            ("p", "The memory manager decides where a process sits in physical memory and keeps processes out of each other's space."),
            ("table", [
                ["Scheme", "External fragmentation", "Internal fragmentation"],
                ["Fixed partitions", "yes", "yes"],
                ["Variable partitions", "yes", "no"],
                ["Paging", "no", "yes, in the last page"],
                ["Segmentation", "yes", "no"],
            ]),
            ("bullets", [
                "First fit: first hole big enough — fastest.",
                "Best fit: smallest adequate hole — leaves tiny unusable holes.",
                "Worst fit: largest hole — generally worst in practice.",
            ]),
            ("key", "Base and limit registers give protection in hardware: every address is checked against the limit and offset by the base, so a process cannot name memory outside its own."),
        ],
    },

    "os_swapping": {
        "title": "Swapping and Fragmentation",
        "blocks": [
            ("def", "Swapping moves a whole process out to backing store and later brings it back, freeing memory for others."),
            ("table", [
                ["", "Internal", "External"],
                ["Where", "inside an allocated block", "between allocated blocks"],
                ["Cause", "fixed-size allocation", "variable-size allocation"],
                ["Cure", "smaller pages", "compaction, or paging"],
            ]),
            ("key", "Paging removes external fragmentation entirely — any free frame will do — at the cost of internal fragmentation averaging half a page per process."),
        ],
    },

    "os_pageing": {
        "title": "Paging",
        "blocks": [
            ("p", "Physical memory is divided into fixed-size frames and logical memory into pages of the same size. A page table maps one to the other, so a process need not be contiguous in physical memory."),
            ("formula", "Logical address = page number p + offset d;  physical address = frame(p) × page size + d"),
            ("example", "With a 16-bit logical address and 4 KB pages, the low 12 bits are the offset and the high 4 bits index a 16-entry page table."),
            ("key", "No external fragmentation, because every free frame is interchangeable. Internal fragmentation is at most one page, in the last page of the process."),
            ("h", "The TLB"),
            ("p", "A plain page table costs two memory accesses per reference: one for the table, one for the data. A translation lookaside buffer caches recent translations."),
            ("formula", "Effective access time = h·(TLB + M) + (1 − h)·(TLB + 2M)"),
            ("warn", "Multi-level page tables shrink the table but add a memory access per level, which makes the TLB hit ratio matter even more."),
        ],
    },

    "os_segmentation": {
        "title": "Segmentation",
        "blocks": [
            ("p", "Memory is divided the way the programmer sees it: code, stack, heap and data as variable-length segments, each with a base and a limit."),
            ("table", [
                ["", "Paging", "Segmentation"],
                ["Block size", "fixed", "variable"],
                ["Visible to programmer", "no", "yes"],
                ["Fragmentation", "internal", "external"],
                ["Address", "page + offset", "segment + offset"],
            ]),
            ("key", "Segmentation matches the logical structure, so protection and sharing can be set per segment — read-only for code, no-execute for the stack."),
            ("note", "Segmented paging combines both: segments are divided into pages, giving logical structure without external fragmentation."),
        ],
    },

    "os_pagereplacement": {
        "title": "Page Replacement Algorithms",
        "blocks": [
            ("p", "On a page fault with no free frame, one resident page must be evicted. Which one decides the fault rate."),
            ("table", [
                ["Algorithm", "Evicts", "Note"],
                ["FIFO", "the oldest page", "can suffer Belady's anomaly"],
                ["Optimal (OPT)", "the page used furthest in the future", "unimplementable, used as a bound"],
                ["LRU", "the least recently used", "good, needs a stack or counters"],
                ["Clock", "approximates LRU with a reference bit", "what real systems use"],
            ]),
            ("key", "Belady's anomaly: with FIFO, more frames can mean more faults. LRU and OPT are stack algorithms and are immune to it."),
            ("example", "Reference string 1,2,3,4,1,2,5,1,2,3,4,5 gives 9 faults with three FIFO frames and 10 with four — the standard demonstration."),
        ],
    },

    "os_vm": {
        "title": "Virtual Memory",
        "blocks": [
            ("def", "Virtual memory runs a process whose logical address space is larger than physical memory, by keeping only the pages in use resident."),
            ("p", "Demand paging brings a page in only when it is referenced. The first touch of each page costs a fault."),
            ("formula", "Effective access time = (1 − p)·memory access + p·page fault time"),
            ("example", "With 100 ns memory, an 8 ms fault service and p = 0.001, the effective access time is about 8100 ns — 81 times slower. Fault rates must be tiny to be tolerable."),
            ("key", "Virtual memory also enables copy-on-write and shared libraries: two processes map the same frame until one writes, and only then is a copy made."),
        ],
    },

    "os_swappingtrashing": {
        "title": "Thrashing",
        "blocks": [
            ("def", "Thrashing is the state where a system spends more time servicing page faults than executing, because processes do not have enough frames to hold their working sets."),
            ("p", "Raising the degree of multiprogramming past that point makes CPU utilisation collapse rather than improve, which looks paradoxical on the utilisation graph."),
            ("key", "The working set model gives each process enough frames for the pages it referenced in the last Δ references. If the working sets do not fit, a process must be suspended."),
            ("note", "Page-fault frequency control is the practical version: give a process more frames when its fault rate is above a high mark, take frames away below a low mark."),
        ],
    },

    "os_discmanagement": {
        "title": "Disk Management",
        "blocks": [
            ("formula", "Disk access time = seek time + rotational latency + transfer time"),
            ("bullets", [
                "Seek time: moving the arm to the right cylinder — the largest and most variable term.",
                "Rotational latency: waiting for the sector, on average half a rotation.",
                "Transfer time: reading the bits once under the head.",
            ]),
            ("example", "At 7200 rpm one rotation takes 8.33 ms, so average rotational latency is about 4.17 ms."),
            ("key", "Because seek dominates, scheduling is about ordering requests by cylinder. That is why every disk scheduling algorithm is a rule for sweeping the arm."),
        ],
    },

    "os_discscheduling": {
        "title": "Disk Scheduling Algorithms",
        "blocks": [
            ("table", [
                ["Algorithm", "Rule", "Weakness"],
                ["FCFS", "request order", "wild arm movement"],
                ["SSTF", "closest cylinder next", "starves distant requests"],
                ["SCAN", "sweep to one end, then reverse", "long wait just behind the head"],
                ["C-SCAN", "sweep one way, jump back", "more uniform waiting"],
                ["LOOK / C-LOOK", "as SCAN but turn at the last request", "the practical versions"],
            ]),
            ("example", "Head at 53, queue 98, 183, 37, 122, 14, 124, 65, 67: FCFS travels 640 cylinders and SSTF 236."),
            ("key", "C-SCAN treats the disk as circular, so the cylinder just behind the head is not penalised by a full return sweep. That fairness is why it is preferred over SCAN."),
        ],
    },
}
