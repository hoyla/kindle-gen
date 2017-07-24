package com.gu.kindlegen

case class Article(
    title: String,
    status: String,
    idString: String,
    urgency: Int, // TODO: Use an enum
    issueDate: Long, // TODO: Use a date/time type
    releaseDate: Long, // TODO: Use a date/time type
    pubDate: Long // TODO: Use a date/time type
) {
  def toNitf: String = s"""
  |<?xml version="1.0" encoding="UTF-8"?>
  |<nitf version="-//IPTC//DTD NITF 3.3//EN">
  |<head>
  |<title>$title</title>
  |<docdata management-status="$status">
  |<doc-id id-string="$idString" />
  |<urgency ed-urg="$urgency" />
  |<date.issue norm="$issueDate" />
  |<date.release norm="$releaseDate" />
  |<doc.copyright holder="guardian.co.uk" />
  |</docdata>
  |<pubdata type="print" date.publication="$pubDate" />  
  |</head>
  |</nitf>""".stripMargin
}
