package com.orientechnologies.orient.server.distributed.impl;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.server.distributed.impl.task.OTransactionPhase1Task;
import com.orientechnologies.orient.server.distributed.impl.task.OTransactionPhase1TaskResult;
import com.orientechnologies.orient.server.distributed.impl.task.transaction.OTxConcurrentModification;
import com.orientechnologies.orient.server.distributed.impl.task.transaction.OTxLockTimeout;
import com.orientechnologies.orient.server.distributed.impl.task.transaction.OTxSuccess;
import com.orientechnologies.orient.server.distributed.impl.task.transaction.OTxUniqueIndex;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestNewDistributedResponseManager {

  @Test
  public void testSimpleQuorum() {
    OTransactionPhase1Task transaction = new OTransactionPhase1Task();
    Set<String> nodes = new HashSet<>();
    nodes.add("one");
    nodes.add("two");
    nodes.add("three");
    ONewDistributedResponseManager responseManager = new ONewDistributedResponseManager(transaction, nodes, nodes, 3, 3, 2);
    assertFalse(responseManager.collectResponse(new OTransactionPhase1TaskResult(new OTxSuccess()), "one"));
    assertTrue(responseManager.collectResponse(new OTransactionPhase1TaskResult(new OTxSuccess()), "two"));
    assertTrue(responseManager.isQuorumReached());
  }

  @Test
  public void testSimpleNoQuorum() {
    OTransactionPhase1Task transaction = new OTransactionPhase1Task();
    Set<String> nodes = new HashSet<>();
    nodes.add("one");
    nodes.add("two");
    nodes.add("three");
    ONewDistributedResponseManager responseManager = new ONewDistributedResponseManager(transaction, nodes, nodes, 3, 3, 2);
    assertFalse(responseManager.collectResponse(new OTransactionPhase1TaskResult(new OTxSuccess()), "one"));
    assertFalse(responseManager
        .collectResponse(new OTransactionPhase1TaskResult(new OTxConcurrentModification(new ORecordId(1, 1), 1)), "two"));
    assertTrue(responseManager.collectResponse(new OTransactionPhase1TaskResult(new OTxLockTimeout()), "two"));
    assertFalse(responseManager.isQuorumReached());
  }

  @Test
  public void testSimpleQuorumLocal() {
    OTransactionPhase1Task transaction = new OTransactionPhase1Task();
    Set<String> nodes = new HashSet<>();
    nodes.add("one");
    nodes.add("two");
    nodes.add("three");
    ONewDistributedResponseManager responseManager = new ONewDistributedResponseManager(transaction, nodes, nodes, 3, 3, 2);
    assertFalse(responseManager.setLocalResult("one", new OTxSuccess()));
    assertTrue(responseManager.collectResponse(new OTransactionPhase1TaskResult(new OTxSuccess()), "one"));
    assertTrue(responseManager.isQuorumReached());
  }

  @Test
  public void testWaitToComplete() throws InterruptedException, ExecutionException {
    ExecutorService executor = Executors.newCachedThreadPool();

    OTransactionPhase1Task transaction = new OTransactionPhase1Task();
    Set<String> nodes = new HashSet<>();
    nodes.add("one");
    nodes.add("two");
    nodes.add("three");
    ONewDistributedResponseManager responseManager = new ONewDistributedResponseManager(transaction, nodes, nodes, 3, 3, 2);
    responseManager.setLocalResult("one", new OTxSuccess());
    CountDownLatch startedWaiting = new CountDownLatch(1);
    Future<Boolean> future = executor.submit(() -> {
      startedWaiting.countDown();
      return responseManager.waitForSynchronousResponses();
    });
    startedWaiting.await();
    assertFalse(future.isDone());
    responseManager.collectResponse(new OTransactionPhase1TaskResult(new OTxSuccess()), "one");
    assertTrue(future.get());
    assertTrue(responseManager.isQuorumReached());
  }

  @Test
  public void testWaitToCompleteNoQuorum() throws InterruptedException, ExecutionException {
    ExecutorService executor = Executors.newCachedThreadPool();

    OTransactionPhase1Task transaction = new OTransactionPhase1Task();
    Set<String> nodes = new HashSet<>();
    nodes.add("one");
    nodes.add("two");
    nodes.add("three");
    ONewDistributedResponseManager responseManager = new ONewDistributedResponseManager(transaction, nodes, nodes, 3, 3, 2);
    responseManager.collectResponse(new OTransactionPhase1TaskResult(new OTxSuccess()), "one");
    assertFalse(responseManager
        .collectResponse(new OTransactionPhase1TaskResult(new OTxConcurrentModification(new ORecordId(1, 1), 1)), "two"));
    CountDownLatch startedWaiting = new CountDownLatch(1);
    Future<Boolean> future = executor.submit(() -> {
      startedWaiting.countDown();
      return responseManager.waitForSynchronousResponses();
    });
    startedWaiting.await();
    assertFalse(future.isDone());
    assertTrue(responseManager.collectResponse(new OTransactionPhase1TaskResult(new OTxLockTimeout()), "one"));
    assertTrue(future.get());
    assertFalse(responseManager.isQuorumReached());
  }

}
