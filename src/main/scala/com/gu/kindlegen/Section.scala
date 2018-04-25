package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.Tag
import com.gu.kindlegen.Link.AbsoluteURL


case class Section(id: String, title: String, link: Link)

object Section {
  def apply(tag: Tag): Section = Section(tag.id, tag.webTitle, AbsoluteURL.from(tag.webUrl))

  implicit val ordering: Ordering[Section] = Ordering.by(section => (section.id, section.title))
}
