package co.datadudes.wsdl2avro

import org.codehaus.jackson.node.NullNode

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
    "anyType" -> Type.STRING
  )

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

  case class BasicNode(name: String, xmlType: String)

  private def removeNS(typeName: String): String = if(typeName contains ":") typeName.split(":")(1) else typeName

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

  def xmlType2Schema(node: Node, parentNode: Option[Node] = None,
                     exclude: BasicNode => Boolean = _ => false): Schema = {
    val elements = sequenceFrom(node)
    val nodeFields = elements
      .filter(_.isTypedXml)
      .filterNot(node => exclude(BasicNode(node.nameAttr, node.typeAttr)))
      .map(element2field)
    val parentNodeFields = parentNode.map(n => sequenceFrom(n)
      .filter(_.isTypedXml)
      .filterNot(node => exclude(BasicNode(node.nameAttr, node.typeAttr)))
      .map(element2field))
    val allFields = parentNodeFields.getOrElse(Nil) ++ nodeFields

    val record = Schema.createRecord(node.nameAttr, null, null, false)
    record.setFields(allFields)
    record
  }

  def element2field(node: Node): Field = {
    val xmlType = removeNS(node.typeAttr)
    val avroType = primitives.getOrElse(xmlType, Type.STRING)

    val (schema, default) = if (node.isOptional) {
      (Schema.createUnion(List(Schema.create(Type.NULL), Schema.create(avroType))), NullNode.getInstance())
    } else if (node.isNullable) {
      (Schema.createUnion(List(Schema.create(avroType), Schema.create(Type.NULL))), null)
    } else {
      (Schema.create(avroType), null)
    }

    new Field(node.nameAttr, schema, null, default)
  }

  def createSchemasFromXMLTypes(typeList: Seq[Node],
                                exclude: BasicNode => Boolean = _ => false): Map[String, Schema] = {
    val (baseTypes, childTypes) = typeList.partition(n => (n \\ "extension").size == 0)
    val baseTypeMap = baseTypes.map(n => n.nameAttr -> n).toMap
    val baseTypeSchemas = baseTypes.map(n => n.nameAttr -> xmlType2Schema(n, None, exclude)).toMap
    val childTypeSchemas = childTypes.map { n =>
      n.nameAttr -> xmlType2Schema(n, baseTypeMap.get(n.baseNodeName), exclude)
    }.toMap
    baseTypeSchemas ++ childTypeSchemas
  }

  def convert(path: String, exclude: BasicNode => Boolean = _ => false): Map[String, Schema] = {
    val types = getDataTypeDefinitions(XML.loadFile(path))
    createSchemasFromXMLTypes(types, exclude)
  }

}
