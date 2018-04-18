package com.gu.contentapi.client.model

import com.gu.contentapi.client.{Parameter, Parameters}

object PrintSentSearchQuery {
  def apply(parameterHolder: Map[String, Parameter] = Map.empty): PrintSentSearchQuery = {
    new PrintSentSearchQuery(Map.empty).withParameters(parameterHolder)
  }
}

final case class PrintSentSearchQuery private(parameterHolder: Map[String, Parameter])
  extends SearchQueryBase[PrintSentSearchQuery]
     with TierParameter[PrintSentSearchQuery] {

  override def pathSegment: String = "content/print-sent"

  override def withParameters(parameterMap: Map[String, Parameter]) = new PrintSentSearchQuery(
    if (parameterMap.contains(tier.name))
      parameterMap
    else
      parameterMap.updated(tier.name, tier.withValue(Some("internal"))))
}

trait TierParameter[Owner <: Parameters[Owner]] extends Parameters[Owner] { this: Owner =>
  def tier = StringParameter("user-tier")
}
