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

  private def removeNS(typeName: String): String = if(typeName contains ":") typeName.split(":")(1) else typeName

  implicit class RichNode(node: Node) {
    def isTypedXml: Boolean = node.label != "#PCDATA" && node.label != "import" && node.label != "any"
    def isNonEmptyType: Boolean = ((node \\ "sequence").size > 0 || (node \\ "all").size > 0) &&
      ((node \\ "sequence" \\ "element").size > 0 || (node \\ "all" \\ "element").size > 0)
    def isTypedAndNonEmpty = isTypedXml && isNonEmptyType
    def nameAttr: String = (node \ "@name").toString()
    def typeAttr: String = (node \ "@type").toString()
    def baseNodeName: String = removeNS(((node \\ "extension").head \ "@base").toString())
    def isNullable: Boolean = (node \ "@nillable").size > 0 && (node \ "@nillable").toString == "true"
    def isOptional: Boolean = (node \ "@minOccurs").size > 0 && (node \ "@minOccurs").toString().toInt == 0
  }

  private def sequenceFrom(node: Node): Seq[Node] =
    if((node \\ "sequence").size > 0)
      (node \\ "sequence").head.child
    else if ((node \\ "all").size > 0)
      (node \\ "all").head.child
    else Nil

  def getDataTypeDefinitions(wsdl: Elem): Seq[Node] = {
    val schemas = wsdl \ "types" \ "schema"
    schemas.flatMap(n => n.child).filter(_.isTypedAndNonEmpty)
  }

  def xmlType2Schema(node: Node, parentNode: Option[Node] = None): Schema = {
    val elements = sequenceFrom(node)
    val nodeFields = elements.filter(_.isTypedXml).map(element2field)
    val parentNodeFields = parentNode.map(n => sequenceFrom(n).filter(_.isTypedXml).map(element2field))
    val allFields = nodeFields ++ parentNodeFields.getOrElse(Nil)

    val record = Schema.createRecord(node.nameAttr, null, null, false)
    record.setFields(allFields)
    record
  }

  def element2field(node: Node): Field = {
    val xmlType = removeNS(node.typeAttr)
    val avroType = primitives.getOrElse(xmlType, Type.STRING)
    val schema = if(node.isNullable || node.isOptional)
        Schema.createUnion(List(Schema.create(Type.NULL), Schema.create(avroType)))
      else
        Schema.create(avroType)
    new Field(node.nameAttr, schema, null, null)
  }

  def createSchemasFromXMLTypes(typeList: Seq[Node]): Map[String, Schema] = {
    val (baseTypes, childTypes) = typeList.partition(n => (n \\ "extension").size == 0)
    val baseTypeMap = baseTypes.map(n => n.nameAttr -> n).toMap
    val baseTypeSchemas = baseTypes.map(n => n.nameAttr -> xmlType2Schema(n)).toMap
    val childTypeSchemas = childTypes.map { n =>
      n.nameAttr -> xmlType2Schema(n, baseTypeMap.get(n.baseNodeName))
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
