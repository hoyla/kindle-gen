package com.gu.contentapi.client.model

import java.util.Locale

package object v1 {
  implicit class RichTagType(val tagType: TagType) extends AnyVal {
    def id: String = tagType.originalName.replace('_', '-').toLowerCase(Locale.ENGLISH)
  }
}
