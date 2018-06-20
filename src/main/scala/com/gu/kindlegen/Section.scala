package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.Tag
import com.gu.io.Link
import com.gu.io.Link.AbsoluteURL


case class Section(id: String, title: String, link: Link)

case class MainSection(info: Section, subsectionIds: Seq[String])

object Section {
  def from(tag: Tag): Section = Section(tag.id, tag.webTitle, AbsoluteURL.from(tag.webUrl))
}
