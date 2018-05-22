package com.gu.nitf


object HtmlToNitfConfig extends HtmlToNitfConfig {
  def nitf: NitfConfig = NitfConfig
}

trait HtmlToNitfConfig {
  def nitf: NitfConfig

  val supportedNitfTags: Set[String] = nitf.tags -- Set("sub", "sup")  // in NITF, these tags must be inside a <num> tag

  val equivalentNitfTag = Map(
    "b"         -> "em",
    "big"       -> "em",
    "cite"      -> "object.title",
    "code"      -> "pre",
    "dfn"       -> "em",
    "dir"       -> "ul",
    "div"       -> "block",
    "figure"    -> "block",
    "h1"        -> "hl2",  // hl1 is only usable within a <hedline>
    "h2"        -> "hl2",
    "i"         -> "em",
    "mark"      -> "em",
    "samp"      -> "pre",
    "section"   -> "block",
    "summary"   -> "block",
    "tt"        -> "em",
    "u"         -> "em",
    "var"       -> "em"
  )

  // tags that should be removed along with their content
  val blacklist = Set(
    // drawings and image maps
    "area",
    "canvas",
    "img",
    "map",
    "progress",
    "svg",

    // external objects
    "applet",
    "audio",
    "embed",
    "frame",
    "frameset",
    "iframe",
    "noframes",
    "noscript",
    "object",
    "param",
    "script",
    "source",
    "track",
    "video",

    // input forms
    "button",
    "fieldset",
    "form",
    "input",
    "optgroup",
    "option",
    "select",
    "textarea",

    // hidden content
    "del",
    "s",
    "strike",
    "template",

    // styling
    "dialog",
    "font",
    "style"
  )
}
