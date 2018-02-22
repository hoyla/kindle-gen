package com.gu.contentapi.client.model

import com.gu.contentapi.client.{Parameter, Parameters}

object PrintSentSearchQuery {
  def apply(parameterHolder: Map[String, Parameter] = Map.empty): PrintSentSearchQuery = {
    new PrintSentSearchQuery(parameterHolder).tier("internal")
  }
}

final case class PrintSentSearchQuery private(parameterHolder: Map[String, Parameter])
  extends SearchQueryBase[PrintSentSearchQuery]
     with TierParameter[PrintSentSearchQuery] {

  override def pathSegment: String = "content/print-sent"
  override def withParameters(parameterMap: Map[String, Parameter]) = new PrintSentSearchQuery(parameterMap)
}

trait TierParameter[Owner <: Parameters[Owner]] extends Parameters[Owner] { this: Owner =>
  def tier = StringParameter("user-tier")
}
