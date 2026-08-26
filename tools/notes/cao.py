# -*- coding: utf-8 -*-
"""Computer Organisation and Architecture revision notes."""

NOTES = {
    "cao_basic_computer": {
        "title": "Issues in Computer Design",
        "blocks": [
            ("p", "Architecture is what the programmer sees — the instruction set, registers and addressing modes. Organisation is how it is built — the datapath, control and memory hierarchy."),
            ("formula", "CPU time = instruction count × CPI × clock period"),
            ("key", "Every optimisation attacks one of those three terms, and usually worsens another. RISC lowers CPI and raises instruction count; CISC does the reverse."),
            ("note", "Amdahl's law bounds the payoff: speeding up a fraction f of the work by s gives an overall speedup of 1 / ((1 − f) + f/s). Making a rare case fast is wasted effort."),
        ],
    },

    "cao_computerinstructions": {
        "title": "Computer Instructions",
        "blocks": [
            ("table", [
                ["Class", "Examples"],
                ["Data transfer", "LOAD, STORE, MOVE"],
                ["Arithmetic and logic", "ADD, SUB, AND, SHIFT"],
                ["Control", "JMP, BRANCH, CALL, RET"],
                ["System", "trap, halt, mode change"],
            ]),
            ("p", "An instruction names an operation and where its operands live. Everything else about the instruction set follows from that pair of choices."),
            ("key", "Instruction count per program falls as instructions do more, but decoding and execution get slower. That tension is the RISC-CISC argument in one sentence."),
        ],
    },

    "cao_machineinstructions": {
        "title": "Machine Instructions",
        "blocks": [
            ("p", "A machine instruction is the binary encoding the control unit decodes: an opcode plus operand fields."),
            ("table", [
                ["Addresses", "Instruction looks like", "Operands from"],
                ["3", "ADD R1, R2, R3", "all named"],
                ["2", "ADD R1, R2", "one is also the destination"],
                ["1", "ADD R1", "accumulator implied"],
                ["0", "ADD", "stack implied"],
            ]),
            ("example", "Evaluating (A+B)×(C+D) takes 3 instructions on a three-address machine and 7 on a zero-address stack machine, but each stack instruction is far shorter."),
            ("key", "Fewer addresses mean shorter instructions and more of them. Total program size can go either way, which is why the comparison must be computed rather than guessed."),
        ],
    },

    "cao_instructionformate": {
        "title": "Instruction Formats",
        "blocks": [
            ("p", "The instruction word is divided into an opcode field and operand fields. The split fixes how many operations and how many registers the machine can have."),
            ("formula", "bits for opcode + Σ bits per operand field ≤ instruction length"),
            ("example", "A 24-bit instruction with three register fields over 32 registers uses 3 × 5 = 15 bits, leaving 9 for the opcode, so at most 512 operations."),
            ("key", "Expanding opcodes buy more instructions by spending operand bits: a rarely used instruction with fewer operands can take a longer opcode. Counting them is a standard question."),
            ("note", "Fixed-length formats decode in one step and suit pipelining. Variable-length formats pack memory more tightly and complicate every fetch."),
        ],
    },

    "cao_addressingmodes": {
        "title": "Addressing Modes",
        "blocks": [
            ("table", [
                ["Mode", "Effective address", "Memory accesses for the operand"],
                ["Immediate", "the operand is in the instruction", "0"],
                ["Register", "in a register", "0"],
                ["Direct", "the address is in the instruction", "1"],
                ["Register indirect", "the address is in a register", "1"],
                ["Indexed", "base + index register", "1"],
                ["Memory indirect", "the address is at the address given", "2"],
                ["Relative", "PC + displacement", "1"],
            ]),
            ("key", "Counting memory accesses per mode is the most examined point. Immediate and register need none; memory indirect needs two, which is why it is rare in modern machines."),
            ("note", "Relative addressing is what makes position-independent code possible: branches encode a displacement rather than an absolute target."),
        ],
    },

    "cao_instructioncycle": {
        "title": "The Instruction Cycle",
        "blocks": [
            ("steps", [
                "Fetch: MAR ← PC, read memory into MBR, PC ← PC + 1, IR ← MBR.",
                "Decode: work out the operation and the operand locations.",
                "Fetch operands, from registers or memory.",
                "Execute in the ALU.",
                "Write back, and check for interrupts.",
            ]),
            ("key", "The PC is incremented during fetch, before execution. A branch therefore overwrites an already-incremented PC, which is why relative displacements are measured from the next instruction."),
            ("note", "Interrupts are checked at the end of an instruction, not in the middle, so the saved state is always a clean boundary."),
        ],
    },

    "cao_datamanupulation_pc": {
        "title": "Data Transfer Instructions",
        "blocks": [
            ("p", "Data transfer moves bits without changing them: register to register, memory to register, or through an I/O port."),
            ("bullets", [
                "LOAD brings a word from memory into a register; STORE sends it back.",
                "MOVE copies between registers.",
                "PUSH and POP transfer against the stack pointer, adjusting it automatically.",
                "IN and OUT address I/O ports where I/O is not memory-mapped.",
            ]),
            ("key", "Transfer instructions do not set condition flags on most architectures. Testing a value after a MOVE therefore needs an explicit compare."),
        ],
    },

    "cao_pcinstruction": {
        "title": "Program Control Instructions",
        "blocks": [
            ("p", "Control instructions change the PC: unconditionally, conditionally on the flags, or by calling and returning."),
            ("table", [
                ["Flag", "Set when"],
                ["Z", "the result is zero"],
                ["C", "a carry out of the most significant bit"],
                ["S / N", "the result is negative"],
                ["V / O", "signed overflow"],
            ]),
            ("key", "Carry signals unsigned overflow, and the overflow flag signals signed overflow. Choosing the wrong one is the classic error in comparison questions."),
            ("note", "CALL pushes the return address and jumps; RET pops it. Nesting works because the stack is last-in-first-out, which is why recursion needs no special instruction."),
        ],
    },

    "cao_datapath": {
        "title": "The Data Path",
        "blocks": [
            ("def", "The data path is the collection of registers, buses, multiplexers and the ALU through which data actually moves; the control unit decides which of those paths is active each cycle."),
            ("bullets", [
                "Single-bus: cheapest, one transfer per cycle, so most instructions take many cycles.",
                "Multiple-bus: several transfers per cycle, fewer cycles per instruction, more hardware.",
            ]),
            ("key", "Counting cycles for a micro-operation sequence is the standard exercise, and the bus count is what bounds how many of them can happen at once."),
        ],
    },

    "cao_aludatapath": {
        "title": "ALU and Data Path",
        "blocks": [
            ("p", "The ALU performs the arithmetic and logic; its function inputs come from the control unit and its flags feed back into it."),
            ("bullets", [
                "Arithmetic: add, subtract, increment, compare.",
                "Logic: AND, OR, XOR, NOT.",
                "Shift: logical, arithmetic and rotate.",
            ]),
            ("key", "Subtraction reuses the adder: A − B is A + (2's complement of B), which is why hardware needs no separate subtractor, only an inverter and a carry-in of 1."),
        ],
    },

    "cao_hardwiredmicroprogrammed": {
        "title": "Hardwired and Micro-programmed Control",
        "blocks": [
            ("table", [
                ["", "Hardwired", "Micro-programmed"],
                ["Built from", "combinational logic", "a control memory of micro-instructions"],
                ["Speed", "faster", "slower, one lookup per step"],
                ["Changing it", "redesign the logic", "rewrite the micro-program"],
                ["Suits", "RISC", "CISC"],
            ]),
            ("bullets", [
                "Horizontal micro-instructions: one bit per control signal — wide words, no decoding, full parallelism.",
                "Vertical micro-instructions: encoded fields — narrow words, a decoder, less parallelism.",
            ]),
            ("key", "Control memory size is width × number of micro-instructions, and computing it from a signal count is a standard question. Horizontal encoding trades memory for speed."),
        ],
    },

    "cao_risccisc": {
        "title": "RISC and CISC",
        "blocks": [
            ("table", [
                ["", "RISC", "CISC"],
                ["Instruction length", "fixed", "variable"],
                ["Instructions", "few, simple", "many, complex"],
                ["Memory operands", "load and store only", "most instructions"],
                ["Addressing modes", "few", "many"],
                ["Control", "hardwired", "usually micro-programmed"],
                ["Registers", "many", "fewer"],
                ["CPI", "low, near 1", "higher and variable"],
            ]),
            ("key", "RISC moves complexity from the hardware to the compiler. Fixed length and load-store are not aesthetic choices — they are what makes a clean pipeline possible."),
            ("note", "The distinction has blurred: modern x86 decodes complex instructions into RISC-like micro-operations internally, keeping the old interface over a new organisation."),
        ],
    },

    "cao_pipelining": {
        "title": "Pipelining",
        "blocks": [
            ("p", "Overlap instructions by splitting execution into stages, so a new instruction starts each cycle while earlier ones finish."),
            ("formula", "Cycles for n instructions in a k-stage pipeline = k + (n − 1)"),
            ("formula", "Speedup = n·k / (k + n − 1) → k as n grows"),
            ("formula", "Clock period = max stage delay + latch delay"),
            ("example", "A 100 ns non-pipelined instruction split into 5 equal stages with 2 ns latches gives a 22 ns clock and a speedup of 100/22 ≈ 4.55, short of the ideal 5."),
            ("key", "Speedup is limited by the slowest stage, not the average. Balancing stages matters more than shortening any one of them."),
            ("warn", "Hazards break the one-per-cycle rate: structural (a resource clash), data (an operand not ready) and control (a branch not yet resolved)."),
        ],
    },

    "cao_arithmeticinstructionpipeline": {
        "title": "Arithmetic and Instruction Pipelines",
        "blocks": [
            ("bullets", [
                "An instruction pipeline overlaps different instructions across fetch, decode, execute and write-back.",
                "An arithmetic pipeline overlaps the stages of one long operation, such as floating point add: compare exponents, shift, add, normalise, round.",
            ]),
            ("key", "Arithmetic pipelines pay off on vector work, where the same operation streams over many operands and the deep pipeline stays full."),
            ("note", "Both are throughput techniques. Neither reduces the latency of a single operation, which is why a dependent chain sees no benefit at all."),
        ],
    },

    "cao_datahazaed": {
        "title": "Dependencies and Data Hazards",
        "blocks": [
            ("table", [
                ["Hazard", "Pattern", "Also called", "Removable by renaming?"],
                ["RAW", "read after write", "true dependency", "no"],
                ["WAR", "write after read", "anti-dependency", "yes"],
                ["WAW", "write after write", "output dependency", "yes"],
            ]),
            ("key", "Only RAW is a real dependency on data. WAR and WAW are artefacts of reusing a register name, so register renaming removes them entirely."),
            ("h", "Fixes"),
            ("bullets", [
                "Forwarding: route a result from a pipeline latch straight to the stage that needs it.",
                "Stalling: insert bubbles when forwarding cannot help.",
                "Reordering: let the compiler or hardware fill the gap with independent work.",
            ]),
            ("warn", "Forwarding cannot fix a load-use hazard: the value is not read from memory until the memory stage, so one stall cycle remains."),
        ],
    },

    "cao_memoryunit": {
        "title": "Memory and the Hierarchy",
        "blocks": [
            ("table", [
                ["Level", "Typical access", "Managed by"],
                ["Registers", "< 1 ns", "compiler"],
                ["L1 / L2 cache", "1–10 ns", "hardware"],
                ["Main memory", "50–100 ns", "operating system"],
                ["SSD / disk", "0.1–10 ms", "operating system"],
            ]),
            ("table", [
                ["", "SRAM", "DRAM"],
                ["Cell", "6 transistors", "1 transistor + capacitor"],
                ["Refresh", "no", "yes, every few ms"],
                ["Speed", "fast", "slower"],
                ["Density and cost", "low density, expensive", "dense, cheap"],
            ]),
            ("key", "The hierarchy works only because of locality. Without temporal and spatial locality every level below the registers would be a pure cost."),
        ],
    },

    "cao_memorymapping": {
        "title": "Cache Mapping",
        "blocks": [
            ("table", [
                ["Mapping", "A block may go", "Tag comparisons", "Replacement policy"],
                ["Direct", "in exactly one line", "1", "none needed"],
                ["Fully associative", "in any line", "all lines", "needed"],
                ["k-way set associative", "in any line of one set", "k", "needed within the set"],
            ]),
            ("formula", "Address = tag | index | block offset"),
            ("formula", "index bits = log₂(number of sets);  offset bits = log₂(block size)"),
            ("example", "32-bit address, 32-byte blocks, direct-mapped with 512 lines: 5 offset bits, 9 index bits, so the tag is 32 − 14 = 18 bits."),
            ("key", "Increasing associativity cuts conflict misses and lengthens the hit path, because more tags must be compared in parallel."),
        ],
    },

    "cao_cache": {
        "title": "Cache Memory and Locality",
        "blocks": [
            ("def", "A cache is a small fast memory holding recently and nearby used blocks, exploiting temporal and spatial locality."),
            ("formula", "Average access time = h·tc + (1 − h)·tm  (simultaneous access)"),
            ("formula", "Average access time = tc + (1 − h)·tm  (hierarchical access)"),
            ("example", "With tc = 10 ns, tm = 100 ns and h = 0.9, the simultaneous model gives 19 ns."),
            ("h", "The three Cs"),
            ("bullets", [
                "Compulsory: the first reference to a block. Larger blocks help.",
                "Capacity: the working set exceeds the cache. A bigger cache helps.",
                "Conflict: too many hot blocks share an index. More associativity helps.",
            ]),
            ("warn", "Read the question for which model is meant. Simultaneous and hierarchical access give different numbers from the same hit ratio."),
            ("note", "Write-through keeps memory always current and costs bandwidth; write-back defers and needs a dirty bit. Write-back plus write-allocate is the usual pairing."),
        ],
    },

    "cao_iointerface": {
        "title": "I/O Interface",
        "blocks": [
            ("table", [
                ["Technique", "CPU cost", "Suits"],
                ["Programmed I/O", "busy-waits on a status bit", "simple, slow devices"],
                ["Interrupt-driven", "one interrupt per transfer unit", "moderate rates"],
                ["DMA", "set up and completion only", "block transfers"],
            ]),
            ("bullets", [
                "Memory-mapped I/O: device registers occupy memory addresses; ordinary loads and stores reach them.",
                "Isolated I/O: a separate address space with dedicated IN and OUT instructions.",
            ]),
            ("key", "Memory-mapped I/O needs no special instructions but consumes address space. Isolated I/O keeps memory addresses free at the cost of extra opcodes and a control line."),
        ],
    },

    "cao_ioprocessor": {
        "title": "I/O Processors",
        "blocks": [
            ("def", "An I/O processor, or channel, is a small dedicated processor that executes its own I/O program from main memory."),
            ("key", "It differs from DMA in autonomy: DMA performs the one transfer it was configured for, while a channel runs a sequence with its own branching and error handling."),
            ("note", "The CPU starts a channel program and is interrupted only at the end, so a whole scatter-gather transfer costs two interactions."),
        ],
    },

    "cao_dma": {
        "title": "Direct Memory Access",
        "blocks": [
            ("p", "A DMA controller moves data between a device and memory over the bus, without the CPU handling each word."),
            ("steps", [
                "The CPU programs the controller with address, count and direction.",
                "The controller requests the bus and transfers.",
                "On completion it interrupts the CPU.",
            ]),
            ("table", [
                ["Mode", "Bus held for", "Effect on the CPU"],
                ["Burst", "the whole block", "stalled until done"],
                ["Cycle stealing", "one word at a time", "slowed, never stopped"],
                ["Transparent", "only idle cycles", "no slowdown, slowest transfer"],
            ]),
            ("key", "Cycle stealing is the usual compromise. The CPU keeps running from cache while the controller takes occasional bus cycles it was not using anyway."),
            ("warn", "DMA writes bypass the cache, so a stale cache line can hide the new data. Cache coherence with DMA is handled by invalidating or by non-cacheable buffers."),
        ],
    },
}
