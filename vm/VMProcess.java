package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.Hashtable;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch. Called by
	 * <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {

		Processor processor = Machine.processor();

		for (int i = 0; i < processor.getTLBSize(); i++) {
			TranslationEntry entry = processor.readTLBEntry(i);
			entry.valid = false;
			processor.writeTLBEntry(i, entry);
		}

		super.saveState();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {

		super.restoreState();
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 *
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {

		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			for (int i = 0; i < section.getLength(); i++) {

				int vpn = section.getFirstVPN() + i;

				VMKernel.diskPageTable.put(new Pair(getProcessId(), vpn), section);
			}
		}

		return true;

	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		super.unloadSections();
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

			case Processor.exceptionTLBMiss:

				int vpn = processor.readRegister(Processor.regBadVAddr); // this is the vpn that caused the TLB miss

				Pair pair = new Pair(this.getProcessId(), vpn);

				Integer ppn = VMKernel.invertedPageTable.get(pair);
				byte[] buffer = new byte[Processor.pageSize + 10];
				byte[] memory = processor.getMemory();

				if (ppn == null) {
					// page fault

					// check if in swap file
					Integer swapPage = (VMKernel.swapPageTable.get(pair));

					if (VMKernel.freePhysicalPages.size() == 0) {
						// dump memory page into swap space

						TranslationEntry entry = processor.readTLBEntry(0);

						if (entry.dirty) {
							VMKernel.swapFile.write(VMKernel.swapFile.length(), memory, entry.ppn, Processor.pageSize);
						}
						VMKernel.freePhysicalPages.push(entry.ppn);

						entry.valid = false;

						processor.writeTLBEntry(0, entry);

					}
					ppn = VMKernel.freePhysicalPages.removeLast();

					if (swapPage != null) {

						VMKernel.swapFile.read(swapPage, buffer, 0, Processor.pageSize);

						System.arraycopy(buffer, 0, memory, ppn, Processor.pageSize);

					} else {

						CoffSection section = VMKernel.diskPageTable.get(new Pair(getProcessId(), vpn));
						section.loadPage(vpn - section.getFirstVPN(), ppn);

					}
					VMKernel.invertedPageTable.put(new Pair(getProcessId(), vpn), ppn);

					// insert into TLB maybe later

				}

				int replaceIndex = -1; // use replacement policy to figure out this index

				int unusedIndex = -1;

				int cleanIndex = -1;

				// first, search if any invalid entry exists
				for (int i = 0; i < processor.getTLBSize(); i++) {
					TranslationEntry entry = processor.readTLBEntry(i);
					if (!entry.valid) {
						replaceIndex = i;
						break;
					}

					if (!entry.used) {
						unusedIndex = i;

					}

					if (!entry.dirty) {
						cleanIndex = i;
					}
				}

				if (replaceIndex < 0) {

					if (unusedIndex < 0) {
						// no invalid and unused entries found, so try replacing one where

						if (cleanIndex < 0) {
							// worst case, replace first for now
							replaceIndex = 0;
						} else {
							replaceIndex = cleanIndex;
						}

					} else {
						replaceIndex = unusedIndex;
					}

				}

				processor.writeTLBEntry(replaceIndex, new TranslationEntry(vpn, ppn, true, false, false, false));
				break;
			default:
				super.handleException(cause);
				break;
		}
	}

	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';
	private static final char dbgVM = 'v';

}
