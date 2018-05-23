package com.gu.nitf


object NitfConfig extends NitfConfig

/** Information about [[https://www.iptc.org/std/NITF/3.5/documentation/nitf-documentation.html NITF v3.5]] */
trait NitfConfig {
  /** Tags whose content must be one or more of [[blockContentTags]] */
  // actually, some of these tags can have other content but this simplification is currently enough
  val blockTags = Set("abstract", "block", "body.content", "bq")

  /** Tags that may contain blockContentTags */
  val blockContentParentTags: Set[String] = blockTags ++ Set("caption", "dd", "li", "media-caption", "td", "th")

  /** Tags that must be contained within [[blockContentParentTags]] */
  val blockContentTags = Set(
    "block", "bq", "dl", "fn", "hl2", "hr", "media", "nitf-table", "note", "ol", "p", "pre", "table", "ul"
  )

  /** Tags that must contain some content (text or other tags) to be valid */
  val nonEmptyTags = Set("note", "abstract", "dl", "fn", "ol", "tr", "ul")

  val allTags = Set(
    "a",
    "abstract",
    "addressee",
    "alt-code",
    "bibliography",
    "block",
    "body",
    "body.content",
    "body.end",
    "body.head",
    "bq",
    "br",
    "byline",
    "byttl",
    "caption",
    "care.of",
    "chron",
    "city",
    "classifier",
    "col",
    "colgroup",
    "copyrite",
    "copyrite.holder",
    "copyrite.year",
    "correction",
    "country",
    "credit",
    "custom-table",
    "datasource",
    "date.expire",
    "date.issue",
    "date.release",
    "dateline",
    "dd",
    "del-list",
    "delivery.office",
    "delivery.point",
    "denom",
    "distributor",
    "dl",
    "doc-id",
    "doc-scope",
    "doc.copyright",
    "doc.rights",
    "docdata",
    "ds",
    "dt",
    "du-key",
    "ed-msg",
    "em",
    "event",
    "evloc",
    "fixture",
    "fn",
    "frac",
    "frac-sep",
    "from-src",
    "function",
    "head",
    "hedline",
    "hl1",
    "hl2",
    "hr",
    "identified-content",
    "iim",
    "key-list",
    "keyword",
    "lang",
    "li",
    "location",
    "media",
    "media-caption",
    "media-metadata",
    "media-object",
    "media-producer",
    "media-reference",
    "meta",
    "money",
    "name.family",
    "name.given",
    "nitf",
    "nitf-col",
    "nitf-colgroup",
    "nitf-table",
    "nitf-table-metadata",
    "nitf-table-summary",
    "note",
    "num",
    "numer",
    "object.title",
    "ol",
    "org",
    "p",
    "person",
    "postaddr",
    "postcode",
    "pre",
    "pronounce",
    "pubdata",
    "q",
    "region",
    "revision-history",
    "rights",
    "rights.agent",
    "rights.enddate",
    "rights.geography",
    "rights.limitations",
    "rights.owner",
    "rights.startdate",
    "rights.type",
    "series",
    "state",
    "story.date",
    "sub",
    "sublocation",
    "sup",
    "table",
    "table-reference",
    "tagline",
    "tbody",
    "td",
    "tfoot",
    "th",
    "thead",
    "title",
    "tobject",
    "tobject.property",
    "tobject.subject",
    "tr",
    "ul",
    "urgency",
    "virtloc",
  )
}
