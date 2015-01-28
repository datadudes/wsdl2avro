package com.datadudes.wsdl2avro

import java.io.File

import scala.xml.{Elem, XML, Node}
import org.apache.avro.Schema
import org.apache.avro.Schema.Type
import org.apache.avro.Schema.Field
import scala.collection.JavaConversions.seqAsJavaList

object WSDL2Avro {

  val primitives = Map(
    "int" -> Type.INT,
    "double" -> Type.DOUBLE,
    "string" -> Type.STRING,
    "boolean" -> Type.BOOLEAN,
    "dateTime" -> Type.STRING,
    "date" -> Type.STRING,
    "time" -> Type.STRING,
    "base64Binary" -> Type.STRING,
    "anyType" -> Type.BYTES
  )

  type NodeFilter = Node => Boolean

  def filterNodes(f: NodeFilter)(nodes: Seq[Node]): Seq[Node] = nodes.filter(f)

  val realXML = (node: Node) => node.label != "#PCDATA" && node.label != "import" && node.label != "any"

  val nonEmptyTypes = (node: Node) => ((node \\ "sequence").size > 0 || (node \\ "all").size > 0) &&
    ((node \\ "sequence" \\ "element").size > 0 || (node \\ "all" \\ "element").size > 0)

  val complexNonEmptyTypes = (node: Node) => realXML(node) && nonEmptyTypes(node)

  // Remove PCDATA and import stuff
  private def filterNonXML = filterNodes(realXML) _

  private def filterNonComplexEmptyTypes = filterNodes(complexNonEmptyTypes) _

  private def name(node: Node): String = (node \ "@name").toString()

  private def parent(child: Node): String = ((child \\ "extension").head \ "@base").toString().split(":")(1)

  private def sequenceFrom(node: Node): Seq[Node] =
    if((node \\ "sequence").size > 0)
      (node \\ "sequence").head.child
    else if ((node \\ "all").size > 0)
      (node \\ "all").head.child
    else Nil

  private def sequence2fields(sequence: Seq[Node]): Seq[Field] = {
    sequence.map(element2field)
  }

  def getDataTypeDefinitions(wsdl: Elem): Seq[Node] = {
    val schemas = wsdl \ "types" \ "schema"
    filterNonComplexEmptyTypes(schemas.flatMap(n => n.child))
  }

  def xmlType2Schema(node: Node, parentNode: Option[Node] = None): Schema = {
    val elements = sequenceFrom(node)
    val record = Schema.createRecord(name(node), null, null, false)
    val fields = parentNode match {
      case Some(p) => sequence2fields(filterNonXML(sequenceFrom(p))) ++ sequence2fields(filterNonXML(elements))
      case None => sequence2fields(filterNonXML(elements))
    }
    record.setFields(fields)
    record
  }

  def element2field(node: Node): Field = {
    val nodeName = name(node)
    val typeAttr = (node \ "@type").toString()
    val xmlType = if(typeAttr contains ":") typeAttr.split(":")(1) else typeAttr
    primitives.get(xmlType) match {
      case Some(n) => new Field(nodeName, Schema.create(n), null, null)
      case None => new Field(nodeName, Schema.create(Type.STRING), null, null) // Optionally point to the ComplexType instead? (only if "nesting" is required)
    }
  }

  def createSchemasFromXMLTypes(typeList: Seq[Node]): Map[String, Schema] = {
    val (baseTypes, childTypes) = typeList.partition(n => (n \\ "extension").size == 0)
    val baseTypeMap = baseTypes.map(n => name(n) -> n).toMap
    val baseTypeSchemas = baseTypes.map(n => name(n) -> xmlType2Schema(n)).toMap
    val childTypeSchemas = childTypes.map { n =>
      name(n) -> xmlType2Schema(n, baseTypeMap.get(parent(n)))
    }.toMap
    baseTypeSchemas ++ childTypeSchemas
  }

  def convert(path: String): Map[String, Schema] = convert(XML.loadFile(path))

  def convert(wsdlFile: File): Map[String, Schema] = convert(XML.loadFile(wsdlFile))

  def convert(wsdl: Elem): Map[String, Schema] = {
    val types = getDataTypeDefinitions(wsdl)
    createSchemasFromXMLTypes(types)
  }

}
