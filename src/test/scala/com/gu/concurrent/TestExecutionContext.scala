package com.gu.concurrent


/** An execution context that runs on the same thread and throws errors to indicate test failure */
object TestExecutionContext extends DirectExecutionContext {
  override def reportFailure(cause: Throwable): Unit = throw cause

  implicit def instance = this
}
