package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see nachos.threads.Condition
 */



     /*
         * Lock lock; Condition cond(lock) a { lock.acquire(); if(task not done)
         * cond.sleep() lock.release(); } b { lock.acquire(); if(task not done)
         * cond.sleep() lock.release(); } ... e { lock.acquire(); if(task not done)
         * cond.sleep() lock.release(); }
         * 
         * f { lock.acquire(); if(task finished) { cond.wakeAll() } lock.release(); }
         * 
         * 
         * 
         * 
         */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 *
	 * @param conditionLock the lock associated with this condition variable. The
	 *                      current thread must hold this lock whenever it uses
	 *                      <tt>sleep()</tt>, <tt>wake()</tt>, or
	 *                      <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		waitQueue = new LinkedList<KThread>();
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		// Lock waiter = new Lock();
		KThread waiter = KThread.currentThread();
		waitQueue.add(waiter);

		conditionLock.release();
		Machine.interrupt().disable();
		waiter.sleep();
		Machine.interrupt().enable();
		conditionLock.acquire();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		Machine.interrupt().disable();
		if (!waitQueue.isEmpty()) {
			
			waitQueue.removeFirst().ready();
		}
		Machine.interrupt().enable();
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current thread
	 * must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		while (!waitQueue.isEmpty()) {
			wake();
		}
	}

	private Lock conditionLock;
	private LinkedList<KThread> waitQueue;
}
