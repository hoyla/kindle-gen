package com.gu.kindlegen

import com.gu.contentapi.client.model.v1.CapiDateTime
import org.joda.time.DateTime

/**
 * Created by alice_dee on 11/08/2017.
 */

case class SectionHeading(
    title: String,
    titleLink: String
) {
}

case class SectionManifest(
    publicationDate: CapiDateTime,
    buildDate: DateTime,
    sections: Seq[SectionHeading]
) {

  def toManifestContentsPage: String = {
    "string"
  }
}
