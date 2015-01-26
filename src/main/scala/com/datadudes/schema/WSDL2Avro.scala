package com.datadudes.schema

import scala.xml.{Elem, XML, Node}
import org.apache.avro.{SchemaBuilder, Schema}
import org.apache.avro.Schema.Type
import org.apache.avro.Schema.Field
import scala.collection.JavaConversions._

object WSDL2Avro {

  val primitives = Map(
    "xsd:int" -> Type.INT,
    "xsd:double" -> Type.DOUBLE,
    "xsd:string" -> Type.STRING,
    "xsd:boolean" -> Type.BOOLEAN,
    "xsd:dateTime" -> Type.STRING,
    "xsd:date" -> Type.STRING,
    "xsd:time" -> Type.STRING,
    "xsd:base64Binary" -> Type.STRING,
    "xsd:anyType" -> Type.BYTES
  )

  type NodeFilter = Node => Boolean

  def filterNodes(f: NodeFilter)(nodes: Seq[Node]): Seq[Node] = nodes.filter(f)

  val realXML = (node: Node) => node.label != "#PCDATA" && node.label != "import" && node.label != "any"

  val complexTypes = (node: Node) => node.label == "complexType"

  val nonEmptyTypes = (node: Node) => (node \\ "sequence").size > 0 && (node \\ "sequence" \\ "element").size > 0

  val complexNonEmptyTypes = (node: Node) => complexTypes(node) && nonEmptyTypes(node)

  // Remove CDATA and import stuff
  def filterNonXML = filterNodes(realXML) _

  def filterNonComplexEmptyTypes = filterNodes(complexNonEmptyTypes) _

  def getDataTypeDefinitions(wsdl: Elem): Seq[Node] = {
    val schemas = wsdl \ "types" \ "schema"
    // TODO: <element> with embedded <complexType> should not be filtered. ComplexType should be unpacked
    filterNonComplexEmptyTypes(schemas.flatMap(n => n.child))
  }

  def complexType2Schema(node: Node): Schema = {
    val name = (node \ "@name").toString()
    val elements = (node \\ "sequence").head.child
    val record = Schema.createRecord(name, null, null, false)
    record.setFields(sequence2fields(filterNonXML(elements)).toList)
    record
  }
  
  def extendedComplexType2Schema(child: Node, parent: Node): Schema = {
    val name = (child \ "@name").toString()
    val childElements = (child \\ "sequence").head.child
    val parentElements = (parent \\ "sequence").head.child
    val record = Schema.createRecord(name, null, null, false)
    val childFields = sequence2fields(filterNonXML(childElements))
    val parentFields = sequence2fields(filterNonXML(parentElements))
    record.setFields(parentFields ++ childFields)
    record
  }

  def parent(child: Node): String = ((child \\ "extension").head \ "@base").toString().split(":")(1)

  def sequence2fields(sequence: Seq[Node]): Seq[Field] = {
    sequence.map(node2field)
  }

  def node2field(node: Node): Field = {
    val primitiveRegex = "xsd:.*".r
    val name = (node \ "@name").toString()
    val xmlType = (node \ "@type").toString()
    xmlType match {
      case primitiveRegex(_*) => new Field(name, Schema.create(primitives(xmlType)), null, null)
      case _ => new Field(name, Schema.create(Type.STRING), null, null) // Optionally point to the ComplexType instead? (only if "nesting" is required)
    }
  }

  def createSchemasFromXMLTypes(typeList: Seq[Node]): Map[String, Schema] = {
    val (baseTypes, childTypes) = typeList.partition(n => (n \\ "extension").size == 0)
    val baseTypeMap = baseTypes.map(n => (n \ "@name").toString() -> n).toMap
    val baseTypeSchemas = baseTypes.map(n => (n \ "@name").toString() -> complexType2Schema(n)).toMap
    val childTypeSchemas = childTypes.map { n =>
      if(baseTypeMap.contains(parent(n)))
        (n \ "@name").toString() -> extendedComplexType2Schema(n, baseTypeMap(parent(n)))
      else // We've probably discarded the parent because it was empty
        (n \ "@name").toString() -> complexType2Schema(n)
    }.toMap
    baseTypeSchemas ++ childTypeSchemas
  }

  def convert(path: String): Map[String, Schema] = {
    val xml = XML.loadFile(path)
    val types = getDataTypeDefinitions(xml)
    createSchemasFromXMLTypes(types)
  }

}
