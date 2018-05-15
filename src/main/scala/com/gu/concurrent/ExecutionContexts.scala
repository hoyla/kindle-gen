package com.gu.concurrent

import scala.concurrent.ExecutionContext

/** An `ExecutionContext` that runs tasks immediately in the thread that invokes `execute`. */
trait DirectExecutionContext extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = runnable.run()
}

/** An `ExecutionContext` that discards errors reported to `reportFailure`. It is suitable for using with unimportant
  * side effects such as calls to [[scala.concurrent.Future.andThen]].
  */
trait ErrorDiscardingExecutionContext extends ExecutionContext {
  override def reportFailure(cause: Throwable): Unit = {}
}

abstract class ErrorReportingExecutionContext(errorReporter: Throwable => Unit = ExecutionContext.defaultReporter) extends ExecutionContext {
  override def reportFailure(cause: Throwable): Unit = errorReporter(cause)
}

/** An `ExecutionContext` that is suitable for quick side effects, e.g. simple calls to [[scala.concurrent.Future.andThen]].
  *
  * Note that a side effect that takes long to execute will delay the completion of the original future.
  */
object SideEffectsExecutionContext extends DirectExecutionContext with ErrorDiscardingExecutionContext
