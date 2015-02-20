package com.datadudes.wsdl2avro

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.AllExpectations
import scala.xml.XML
import WSDL2Avro._
import scala.collection.JavaConversions._
import org.apache.avro.Schema.Type

class WSDL2AvroSpec extends SpecificationWithJUnit with AllExpectations {

  "WSDL2Avro" should {

    "get the data type definitions from a wsdl" in {
      val url = getClass.getResource("/example.wsdl").toURI.toURL
      val xml = XML.load(url)

      getDataTypeDefinitions(xml).map(node => (node \ "@name").toString()) must contain(
        "TradePriceRequest", "TradePrice", "InternationalTradePrice"
      )
      getDataTypeDefinitions(xml).map(node => (node \ "@name").toString()) must not contain "EmptyType"
    }

    "exclude fields based on the supplied predicate" in {
      val url = getClass.getResource("/example.wsdl").toURI.toURL
      val xml = XML.load(url)
      val types = getDataTypeDefinitions(xml)
      val predicate = (node: BasicNode) => node.name.contains("exchange")
      val schema = xmlType2Schema(types(0), None, predicate)

      schema.getFields.size() must be equalTo 1
      schema.getFields.toList.map(_.name()) must not contain "exchange"
    }

    "combine parent and child fields in case of inheritance" in {
      val parent = <complexType name="TradePrice">
                    <sequence>
                      <element name="price" type="float"/>
                    </sequence>
                  </complexType>
      val child = <complexType name="InternationalTradePrice">
                    <complexContent>
                      <extension base="xsd1:TradePrice">
                        <sequence>
                          <element name="currency" type="string"/>
                        </sequence>
                      </extension>
                    </complexContent>
                  </complexType>

      val elementNames = List(parent, child)
        .flatMap(node => (node \\ "sequence").head.child)
        .filter(node => node.label != "#PCDATA")
        .map(n => (n \ "@name").toString())
      val schema = xmlType2Schema(child, Some(parent))

      schema.getFields.toList.map(_.name()) must containTheSameElementsAs(elementNames)
    }

    "not care about namespace prefixes" in {
      val element1 = <element name="element1" type="xsd:int"/>
      val element2 = <element name="element2" type="boolean"/>
      val element3 = <element name="element3" type="foobar:string"/>

      element2field(element1).schema().getType must be equalTo Type.INT
      element2field(element2).schema().getType must be equalTo Type.BOOLEAN
      element2field(element3).schema().getType must be equalTo Type.STRING
    }

    "convert XML primitives to the right Avro primitives" in {
      val intElement = <element name="intElement" type="int"/>
      val doubleElement = <element name="doubleElement" type="double"/>
      val stringElement = <element name="stringElement" type="string"/>
      val booleanElement = <element name="booleanElement" type="boolean"/>
      val dateTimeElement = <element name="dateTimeElement" type="dateTime"/>
      val dateElement = <element name="dateElement" type="date"/>
      val timeElement = <element name="timeElement" type="time"/>
      val base64BinaryElement = <element name="base64BinaryElement" type="base64Binary"/>
      val anyTypeElement = <element name="anyTypeElement" type="anyType"/>

      element2field(intElement).schema().getType must be equalTo Type.INT
      element2field(doubleElement).schema().getType must be equalTo Type.DOUBLE
      element2field(stringElement).schema().getType must be equalTo Type.STRING
      element2field(booleanElement).schema().getType must be equalTo Type.BOOLEAN
      element2field(dateTimeElement).schema().getType must be equalTo Type.STRING
      element2field(dateElement).schema().getType must be equalTo Type.STRING
      element2field(timeElement).schema().getType must be equalTo Type.STRING
      element2field(base64BinaryElement).schema().getType must be equalTo Type.STRING
      element2field(anyTypeElement).schema().getType must be equalTo Type.STRING
    }

    "convert XML complex types to Avro strings" in {
      val element1 = <element name="element1" type="Foo"/>
      val element2 = <element name="element2" type="ens:Bar"/>

      element2field(element1).schema().getType must be equalTo Type.STRING
      element2field(element2).schema().getType must be equalTo Type.STRING
    }

  }

}
