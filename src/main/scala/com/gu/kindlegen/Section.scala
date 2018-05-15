package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.Tag
import com.gu.io.Link
import com.gu.io.Link.AbsoluteURL


case class Section(id: String, title: String, link: Link)

object Section {
  def apply(tag: Tag): Section = Section(tag.id, tag.webTitle, AbsoluteURL.from(tag.webUrl))
}
