package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.Tag
import com.gu.io.Link
import com.gu.io.Link.AbsoluteURL


case class Section(id: String, title: String, link: Link)

case class MainSectionTemplate(id: String,
                               title: Option[String] = None,
                               link: Option[Link] = None,
                               overrides: Seq[String] = Nil) {
  val sectionIds = id +: overrides

  def withDefaults(title: String, link: Link) =
    Section(id, this.title.getOrElse(title), this.link.getOrElse(link))
}

object Section {
  def from(tag: Tag): Section = Section(tag.id, tag.webTitle, AbsoluteURL.from(tag.webUrl))
}
