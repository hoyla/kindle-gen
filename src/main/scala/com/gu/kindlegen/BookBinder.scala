package com.gu.kindlegen

import scala.collection.{breakOut, mutable}


trait BookBinder {
  def group(articles: Seq[Article]): Seq[BookSection]
}

class MainSectionsBookBinder(mainSections: Seq[MainSection]) extends BookBinder {
  override def group(articles: Seq[Article]): Seq[BookSection] = {
    articles.groupBy(mainSection)
      .map { case (section, sectionArticles) => BookSection(section, sectionArticles) }(breakOut)
      .sorted(bookSectionOrdering)
  }

  def mainSection(article: Article): Section = {
    mainSectionInfo.getOrElse(article.section.id,
      unknownSections.getOrElseUpdate(article.section.id, article.section))
  }

  private type SectionId = String

  private val mainSectionInfo: Map[SectionId, Section] =
    mainSections.iterator.flatMap(m => (m.info.id +: m.subsectionIds).map((_, m.info))).toMap

  // Keep track of a unique definition of a section, even if articles have sections with common ids and different titles.
  // This is to work around `groupBy` comparing all fields of the Section case class.
  private val unknownSections = mutable.Map.empty[SectionId, Section]

  // sections should be sorted according to their order in `mainSections`,
  // with unknown ones falling at the end and sorted according to their page numbers
  private val bookSectionOrdering = new Ordering[BookSection] {
    private val sectionOrder =
      mainSections.iterator.map(_.info.id).zipWithIndex.toMap.withDefaultValue(Int.MaxValue)

    override def compare(x: BookSection, y: BookSection): Int = {
      val i = sectionOrder(x.section.id)
      val j = sectionOrder(y.section.id)

      if (i != j) i - j
      else BookSection.ordering.compare(x, y)  // both are unknown
    }
  }
}
