package com.gu.kindlegen

import scala.collection.breakOut

import org.apache.logging.log4j.scala.Logging


trait BookBinder {
  def group(articles: Seq[Article]): Seq[BookSection]
}


object MainSectionsBookBinder {
  /** A binder that uses the same sections attached to articles */
  val default = new MainSectionsBookBinder(Seq.empty)

  def apply(mainSectionTemplates: Seq[MainSectionTemplate]) =
    new MainSectionsBookBinder(mainSectionTemplates)
}

class MainSectionsBookBinder(mainSectionTemplates: Seq[MainSectionTemplate]) extends BookBinder with Logging {
  private type SectionId = String

  override def group(articles: Seq[Article]): Seq[BookSection] = {
    val mainSections: Map[SectionId, Section] = mainSectionMappings(articles)

    articles.groupBy(article => mainSections(article.section.id))
      .map { case (section, sectionArticles) => BookSection(section, sectionArticles) }(breakOut)
      .sorted(bookSectionOrdering)
  }

  private def mainSectionMappings(articles: Seq[Article]): Map[SectionId, Section] = {
    val articleSections: Map[SectionId, Section] = articles.map { a => a.section.id -> a.section }(breakOut)

    val mainSections = mainSectionTemplates.flatMap { mainSection =>
      val maybeSection = sectionInfo(mainSection, articleSections)
      maybeSection.toIterable.flatMap { section =>
        mainSection.sectionIds.map(_ -> section)
      }
    }

    logger.debug(s"mainSectionTemplates:\n\t${mainSectionTemplates.mkString("\n")}")
    logger.debug(s"mainSections:\n\t${mainSections.mkString("\n\t")}")

    articleSections ++ mainSections  // keep unknown sections and override the ids for main sections
  }

  /** Looks up section information for the main section.
    * If the result is `None` then the section is not in `articleSections` and is not needed.
    */
  private def sectionInfo(mainSection: MainSectionTemplate, articleSections: Map[SectionId, Section]): Option[Section] = {
    mainSection match {
      case MainSectionTemplate(id, Some(title), Some(link), _) =>
        Some(Section(id, title, link))
      case _ =>
        val maybeSectionInfo = mainSection.sectionIds.toStream.flatMap(articleSections.get).headOption
        maybeSectionInfo.map { section => mainSection.withDefaults(section.title, section.link) }
    }
  }

  // sections should be sorted according to their order in `mainSections`,
  // with unknown ones falling at the end and sorted according to their page numbers
  private val bookSectionOrdering = new Ordering[BookSection] {
    private val sectionOrder =
      mainSectionTemplates.iterator.flatMap(_.sectionIds).zipWithIndex.toMap.withDefaultValue(Int.MaxValue)

    logger.debug(s"sectionOrder: ${sectionOrder.keys}")

    override def compare(x: BookSection, y: BookSection): Int = {
      val i = sectionOrder(x.section.id)
      val j = sectionOrder(y.section.id)

      if (i != j) i - j
      else BookSection.ordering.compare(x, y)  // both are unknown
    }
  }
}
