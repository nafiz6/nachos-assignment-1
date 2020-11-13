package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {

        processCountLock = new Lock();
        activeProcessesLock = new Lock();

        int numPhysPages = Machine.processor().getNumPhysPages();
        childProcessesId = new ArrayList<>();

        processCountLock.acquire();
        processId = processCount;
        processCount++;
        processIdMap.put(processId, this);

        processCountLock.release();

        exitStatus = -1;

        stdIn = UserKernel.console.openForReading();
        stdOut = UserKernel.console.openForWriting();

        // on context switches, this pageTable gets saved onto processor

        pageTable = new TranslationEntry[numPhysPages];

        for (int i = 0; i < numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i, i, true, false, false, false);

        // added code
        if (rootProcess == null) {
            rootProcess = this;
        }
    }

    /**
     * Allocate and return a new process of the correct class. The class name is
     * specified by the <tt>nachos.conf</tt> key <tt>Kernel.processClassName</tt>.
     *
     * @return a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to load
     * the program, and then forks a thread to run it.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
            return false;

        uThread = new UThread(this).setName(name);
        uThread.fork();
        activeProcessesLock.acquire();
        activeProcesses++;
        activeProcessesLock.release();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch. Called by
     * <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read at
     * most <tt>maxLength + 1</tt> bytes from the specified address, search for the
     * null terminator, and convert it to a <tt>java.lang.String</tt>, without
     * including the null terminator. If no null terminator is found, returns
     * <tt>null</tt>.
     *
     * @param vaddr     the starting virtual address of the null-terminated string.
     * @param maxLength the maximum number of characters in the string, not
     *                  including the null terminator.
     * @return the string read, or <tt>null</tt> if no null terminator was found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length = 0; length < bytesRead; length++) {
            if (bytes[length] == 0)
                return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param data  the array where the data will be stored.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array. This
     * method handles address translation details. This method must <i>not</i>
     * destroy the current process if an error occurs, but instead should return the
     * number of bytes successfully copied (or zero if no data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to read.
     * @param data   the array where the data will be stored.
     * @param offset the first byte to write in the array.
     * @param length the number of bytes to transfer from virtual memory to the
     *               array.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {

        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        // here offset if the starting point of the destination (ie data) array

        // this reads length bytes from virtual memory.

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses

        // if (vaddr < 0 || vaddr >= memory.length)
        // return 0;

        // added
        if (vaddr < 0 || vaddr > getMaxVirtualAddr())
            return 0;
        // end

        // int amount = Math.min(length, memory.length - vaddr);
        // added
        int amount = Math.min(length, getMaxVirtualAddr() - vaddr + 1);
        // so if length is such that writing exceeds
        // physical/virtual memory bounds, then only read upto
        // max memory index
        // added

        int lastPage = (vaddr + amount) / pageSize;
        int firstPage = vaddr / pageSize;
        int pagesToRead = lastPage - firstPage + 1;

        int startOffset = vaddr % pageSize;

        int remaining = amount;

        int bytesRead = 0;

        for (int i = 0; i <= pagesToRead; i++) {

            int vpn = firstPage + i;

            // ----- ----- ----- ----- ----- -----



            // remaining = Math.min(pageSize - startOffset, amount);

            // remaining = amount - (i * pageSize);


          
            // System.out.println("REMAINING: " + remaining);
            int readLimit = Math.min(pageSize, remaining);

            if(i == 0) {
                readLimit = Math.min(pageSize - startOffset - 1 , amount);
                
            }

            remaining -= readLimit;
            

            pageTable[vpn].used = true;

            int start = pageTable[vpn].ppn * pageSize; // if page isnt first page, start will always be start of page
            if (i == 0) {
                // this is first page accessed, so take startOffset into account
                start += startOffset;

                // if (readLimit == pageSize) {
                //     readLimit = readLimit - startOffset;
                // }

            }

           

            System.arraycopy(memory, start, data, offset + bytesRead, readLimit);
            bytesRead += readLimit;

        }

        // System.arraycopy(memory, vaddr, data, offset, amounVt);

        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data  the array containing the data to transfer.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory. This
     * method handles address translation details. This method must <i>not</i>
     * destroy the current process if an error occurs, but instead should return the
     * number of bytes successfully copied (or zero if no data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to write.
     * @param data   the array containing the data to transfer.
     * @param offset the first byte to transfer from the array.
     * @param length the number of bytes to transfer from the array to virtual
     *               memory.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {

        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
        // System.out.println("WRITE VM USED");
        // System.out.flush();

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
        // if (vaddr < 0 || vaddr >= memory.length)
        // return 0;

        if (vaddr < 0 || vaddr > getMaxVirtualAddr())
            return 0;

        // int amount = Math.min(length, memory.length - vaddr);

        int amount = Math.min(length, getMaxVirtualAddr() - vaddr + 1);

        // for (int i = offset; i < amount; i++) {
        // memory[pageTable[vaddr + i].ppn] = data[i];

        // }

        // int bytesWritten = amount;

        int pagesToRead = amount / pageSize;
        int firstPage = vaddr / pageSize;

        int startOffset = vaddr % pageSize;

        int remaining = amount;

        int bytesWritten = 0;

        for (int i = 0; i <= pagesToRead; i++) {

            // if (pageTable[i + vaddr].readOnly) {
            // // cant write to this page, so skip
            // bytesWritten = bytesWritten - pageSize;
            // continue;
            // }



            int vpn = firstPage + i;

            int writeLimit = Math.min(pageSize, remaining);

            if(i == 0) {
                writeLimit = Math.min(pageSize - startOffset - 1, amount);
                
            }

            remaining -= writeLimit;




            // int remaining = amount - (i * pageSize);
            // int writeLimit = Math.min(pageSize, remaining);

            pageTable[vpn].used = true;
            pageTable[vpn].dirty = true;

            int start = pageTable[vpn].ppn * pageSize; // if page isnt first page, start will always be start of page
            if (i == 0) {
                // this is first page accessed, so take startOffset into account
                start += startOffset;

                // if (writeLimit == pageSize) {
                //     writeLimit = writeLimit - startOffset;
                // }

            }

            System.arraycopy(data, offset + bytesWritten, memory, start, writeLimit);
            bytesWritten += writeLimit;

        }

        // System.arraycopy(data, offset, memory, vaddr, amount);

        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and prepare to
     * pass it the specified arguments. Opens the executable, reads its header
     * information, and copies sections and arguments into this process's virtual
     * memory.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        } catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i = 0; i < args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages * pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages - 1) * pageSize;
        int stringOffset = entryOffset + args.length * 4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i = 0; i < argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into memory.
     * If this returns successfully, the process will definitely be run (this is the
     * last step in process initialization that can fail).
     *
     * @return <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {

        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

        // add
        // setup pageTable here, which maps i vpn to i ppn in constructor
        // get free spaces from UKernel.freePages, and add these to pageTable's entries'
        // ppn's
        // int stackPages = 8; // this many pages are allocated for this process apart
        // from the space it
        // already ends up taking at load time, not sure about this yet

        UserKernel.lock.acquire();
        if (numPages > UserKernel.freePhysicalPages.size()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            UserKernel.lock.release();
            return false;

        }
        UserKernel.lock.release();

        for (int i = 0; i < pageTable.length; i++) {

            if (i >= numPages) {
                pageTable[i].valid = false;

            } else {
                UserKernel.lock.acquire();

                pageTable[i].ppn = UserKernel.freePhysicalPages.remove(17 % UserKernel.freePhysicalPages.size());

                UserKernel.lock.release();
            }
        }
        // end

        // load sections

        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess,
                    "\tinitializing " + section.getName() + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {

                int vpn = section.getFirstVPN() + i; // we assume that they know that our pageTable config

                // add
                pageTable[vpn].readOnly = section.isReadOnly();
                // pageTable[vpn].valid = section.isInitialzed(); // not sure about this yet

                int ppn = pageTable[vpn].ppn;

                // end
                // for now, just assume virtual addresses=physical addresses
                // section.loadPage(i, vpn);
                // add
                section.loadPage(i, ppn);

                // end
            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        // idk if we should implement this, if we do, add pages to free pages linked
        // list here
    }

    /**
     * Initialize the processor's registers in preparation for running the program
     * loaded into this process. Set the PC register to point at the start function,
     * set the stack pointer register to point at the top of the stack, set the A0
     * and A1 registers to argc and argv, respectively, and initialize all other
     * registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i = 0; i < processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call.
     */
    private int handleHalt() {
        // added code
        if (this != rootProcess)
            return 0;

        Machine.halt();

        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    // added code
    /**
     * Handle the read() system call.
     */
    private int handleRead(int fileDescriptor, int bufferAddr, int size) {
        if (fileDescriptor != 0)
            return -1;

        if (size < 0) {
            return -1;
        }

        if (bufferAddr < 0 || bufferAddr > getMaxVirtualAddr()) {
            return -1;
        }

        // byte[] buf = Machine.processor().getMemory();
        byte[] buf = new byte[size];

        UserKernel.consoleLock.acquire();
        int readSize = stdIn.read(buf, 0, size);
        UserKernel.consoleLock.release();
        int bytesWritten = writeVirtualMemory(bufferAddr, buf);

        // System.out.println("TESTINGGG " + readVirtualMemoryString(bufferAddr, size));

        return bytesWritten;
    }

    /**
     * Handle the write() system call.
     */
    private int handleWrite(int fileDescriptor, int bufferAddr, int size) {
        // System.out.println("BUFFADDR write" + bufferAddr);//5120
        if (fileDescriptor != 1)
            return -1;

        if (size < 0) {
            return -1;
        }

        if (bufferAddr < 0 || bufferAddr > getMaxVirtualAddr()) {
            return -1;
        }

        // byte[] buf = Machine.processor().getMemory();
        byte[] buf = new byte[size];
        int amountRead = readVirtualMemory(bufferAddr, buf);
        UserKernel.consoleLock.acquire();
        int writeSize = stdOut.write(buf, 0, amountRead);
        UserKernel.consoleLock.release();
        return writeSize;
    }

    /**
     * Handle the exec() system call.
     */
    private int handleExec(int fileAddr, int argc, int argvAddr) {
        // System.out.println("HANDLE EXEC CALLED");
        String filename = readVirtualMemoryString(fileAddr, getMaxVirtualAddr() - fileAddr + 1);
        // System.out.println("FILENAMESTART" + filename + "FILENAMEEND");
        if (filename == null)
            return -1;
        // check filename ending with coff
        if (!filename.endsWith(".coff"))
            return -1;

        if (argc < 0)
            return -1;
        // System.out.println("FILENAME " + filename);

        String args[] = new String[argc];
        for (int i = 0; i < argc; i++) {
            args[i] = readVirtualMemoryString(argvAddr, getMaxVirtualAddr() - argvAddr + 1);

            if (args[i] == null)
                return -1;

            argvAddr += args[i].getBytes().length;
        }
        // System.out.println("READ ARGS");

        UserProcess process = UserProcess.newUserProcess();
        process.parentProcess = this;
        childProcessesId.add(process.processId);

        boolean created = process.execute(filename, args);
        if (!created)
            return -1;

        return process.getProcessId();
    }

    /**
     * Handle the join() system call.
     */
    private int handleJoin(int processIdToJoin, int statusAddr) {
        UserProcess toJoin = processIdMap.get(processIdToJoin);
        // check if child
        boolean isChild = childProcessesId.contains((Integer) processIdToJoin);
        if (!isChild)
            return -1;

        // wait until process over?
        toJoin.uThread.join();
        // System.out.println("JOIN ENDED");

        // remove from childproccesses
        childProcessesId.remove((Integer) processIdToJoin);

        Integer childExitStatus = toJoin.exitStatus;

        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(childExitStatus);

        writeVirtualMemory(statusAddr, bb.array());

        if (childExitStatus == 0)
            return 1;
        return 0;
    }

    /**
     * Handle the write() system call.
     */
    private void handleExit(int status) {

        for (int childProcessId : childProcessesId) {
            UserProcess childProcess = processIdMap.get(childProcessId);
            childProcess.parentProcess = null;
        }
        childProcessesId = null;
        if (!(rootProcess == this))
            parentProcess.childProcessesId.remove((Integer) processId);

        exitStatus = status;
        this.stdIn.close();
        this.stdOut.close();

        for (TranslationEntry tEntry : pageTable) {
            if (tEntry.valid) {
                UserKernel.lock.acquire();
                UserKernel.freePhysicalPages.add(tEntry.ppn);
                UserKernel.lock.release();
            }
        }

        activeProcessesLock.acquire();
        activeProcesses--;
        activeProcessesLock.release();

        uThread.wakeSleepingThread();

        activeProcessesLock.acquire();

        if (activeProcesses == 0) {
            activeProcessesLock.release();
            Kernel.kernel.terminate();

        }
        activeProcessesLock.release();

        KThread.finish();

    }

    private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2, syscallJoin = 3, syscallCreate = 4,
            syscallOpen = 5, syscallRead = 6, syscallWrite = 7, syscallClose = 8, syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr>
     * <td>syscall#</td>
     * <td>syscall prototype</td>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td><tt>void halt();</tt></td>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td><tt>void exit(int status);</tt></td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td><tt>int  join(int pid, int *status);</tt></td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td><tt>int  creat(char *name);</tt></td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td><tt>int  open(char *name);</tt></td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td>
     * </tr>
     * <tr>
     * <td>7</td>
     * <td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td>
     * </tr>
     * <tr>
     * <td>8</td>
     * <td><tt>int  close(int fd);</tt></td>
     * </tr>
     * <tr>
     * <td>9</td>
     * <td><tt>int  unlink(char *name);</tt></td>
     * </tr>
     * </table>
     * 
     * @param syscall the syscall number.
     * @param a0      the first syscall argument.
     * @param a1      the second syscall argument.
     * @param a2      the third syscall argument.
     * @param a3      the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallRead:
                return handleRead(a0, a1, a2);
            case syscallWrite:
                return handleWrite(a0, a1, a2);
            case syscallExec:
                return handleExec(a0, a1, a2);
            case syscallJoin:
                return handleJoin(a0, a1);
            case syscallExit:
                handleExit(a0);
                break;

            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>.
     * The <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                        processor.readRegister(Processor.regA0), processor.readRegister(Processor.regA1),
                        processor.readRegister(Processor.regA2), processor.readRegister(Processor.regA3));
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;

            default:
                System.out.println("UNHANDLED EXCEPTION");
                handleExit(-1);
                Lib.debug(dbgProcess, "Unexpected exception: " + Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    public int getMaxVirtualAddr() {
        return numPages * pageSize - 1;
    }

    public int getProcessId() {
        return processId;
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    // added
    private static UserProcess rootProcess = null;

    public UserProcess parentProcess;
    public List<Integer> childProcessesId;

    public static int processCount = 0;
    public static int activeProcesses = 0;
    public static HashMap<Integer, UserProcess> processIdMap = new HashMap<>();
    private int processId;
    public KThread uThread;

    private OpenFile stdIn, stdOut;

    public int exitStatus;

    public static Lock processCountLock;
    public static Lock activeProcessesLock;

}
