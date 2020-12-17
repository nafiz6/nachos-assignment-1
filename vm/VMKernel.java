package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.Hashtable;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {

    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
        super();
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
        

        super.initialize(args);
        swapFile = Machine.stubFileSystem().open("swap", true);
    }

    /**
     * Test this kernel.
     */
    public void selfTest() {
        super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {
        super.run();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
        super.terminate();
    }

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    public static Hashtable<Pair, Integer> invertedPageTable = new Hashtable<>();
    public static Hashtable<Pair, CoffSection> diskPageTable = new Hashtable<>();
    public static Hashtable<Pair, Integer> swapPageTable = new Hashtable<>();
    

   

    public static OpenFile swapFile; 

}



// class MMU {

//     int getPhysicalAddress(int vaddr) {

//         Processor processor = Machine.processor();
//         int vpn = Processor.pageFromAddress(vaddr);
//         int vOffset = Processor.offsetFromAddress(vaddr);

//         for (int i = 0; i < processor.getTLBSize(); i++) {
//             if (translations[i].valid && translations[i].vpn == vpn) {
//                 entry = translations[i];
//                 break;
//             }
//         }
//         if (entry == null) {
//             privilege.stats.numTLBMisses++;
//             Lib.debug(dbgProcessor, "\t\tTLB miss");
//             throw new MipsException(exceptionTLBMiss, vaddr);
//         }

//         return 0;
//     }

// }
