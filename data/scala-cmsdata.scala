// class definitions

trait LorentzVector extends Serializable {
  // abstract members; must be defined by subclasses
  def px: Double
  def py: Double
  def pz: Double
  def E: Double

  // methods common to all LorentzVectors
  def pt = Math.sqrt(px*px + py*py)
  def p = Math.sqrt(px*px + py*py + pz*pz)
  def mass = Math.sqrt(E*E - px*px - py*py - pz*pz)
  def eta = 0.5*Math.log((p + pz)/(p - pz))
  def phi = Math.atan2(py, px)

  // addition operator is a method named "+"
  def +(two: LorentzVector) = {
    val one = this
    // create a subclass and an instance in one block
    new LorentzVector {
      def px = one.px + two.px
      def py = one.py + two.py
      def pz = one.pz + two.pz
      def E = one.E + two.E
      override def toString() = s"LorentzVector($px, $py, $pz, $E)"
    }
  }
}

// particle class definitions are now one-liners

case class Jet(px: Double, py: Double, pz: Double, E: Double, btag: Double) extends LorentzVector

case class Muon(px: Double, py: Double, pz: Double, E: Double, q: Int, iso: Double) extends LorentzVector

case class Electron(px: Double, py: Double, pz: Double, E: Double, q: Int, iso: Double) extends LorentzVector

case class Photon(px: Double, py: Double, pz: Double, E: Double, iso: Double) extends LorentzVector

case class MET(px: Double, py: Double) {
  def pt = Math.sqrt(px*px + py*py)
}

case class Event(jets: Seq[Jet], muons: Seq[Muon], electrons: Seq[Electron], photons: Seq[Photon], met: MET, numPrimaryVertices: Long)

// event data iterator
case class EventIterator(location: String = "http://histogrammar.org/docs/data/triggerIsoMu24_50fb-1.json.gz") extends Iterator[Event] {
  import org.dianahep.histogrammar.json._

  // use Java libraries to stream and decompress data on-the-fly
  @transient val scanner = new java.util.Scanner(
    new java.util.zip.GZIPInputStream(
      new java.net.URL(location).openStream))

  // read one ahead so that hasNext can effectively "peek"
  private def getNext() =
    try {
      Json.parse(scanner.nextLine) collect {
        case event: JsonObject => eventFromJson(event)
      }
    }
    catch {
      case err: java.util.NoSuchElementException => None
    }

  private var theNext = getNext()

  // iterator interface
  def hasNext = !theNext.isEmpty
  def next() = {
    val out = theNext.get
    theNext = getNext()
    out
  }

  def jetFromJson(params: Map[String, JsonNumber]) =
    new Jet(params("px").toDouble,
            params("py").toDouble,
            params("pz").toDouble,
            params("E").toDouble,
            params("btag").toDouble)

  def muonFromJson(params: Map[String, JsonNumber]) =
    new Muon(params("px").toDouble,
             params("py").toDouble,
             params("pz").toDouble,
             params("E").toDouble,
             params("q").toInt,
             params("iso").toDouble)

  def electronFromJson(params: Map[String, JsonNumber]) =
    new Electron(params("px").toDouble,
                 params("py").toDouble,
                 params("pz").toDouble,
                 params("E").toDouble,
                 params("q").toInt,
                 params("iso").toDouble)

  def photonFromJson(params: Map[String, JsonNumber]) =
    new Photon(params("px").toDouble,
               params("py").toDouble,
               params("pz").toDouble,
               params("E").toDouble,
               params("iso").toDouble)

  def metFromJson(params: Map[String, JsonNumber]): MET =
    new MET(params("px").toDouble, params("py").toDouble)

  def eventFromJson(params: JsonObject) = {
    val JsonArray(jets @ _*) = params("jets")
    val JsonArray(muons @ _*) = params("muons")
    val JsonArray(electrons @ _*) = params("electrons")
    val JsonArray(photons @ _*) = params("photons")
    val met = params("MET").asInstanceOf[JsonObject]
    val JsonInt(numPrimaryVertices) = params("numPrimaryVertices")
    new Event(
      jets collect {case j: JsonObject => jetFromJson(j.to[JsonNumber].toMap)},
      muons collect {case j: JsonObject => muonFromJson(j.to[JsonNumber].toMap)},
      electrons collect {case j: JsonObject => electronFromJson(j.to[JsonNumber].toMap)},
      photons collect {case j: JsonObject => photonFromJson(j.to[JsonNumber].toMap)},
      metFromJson(met.to[JsonNumber].toMap),
      numPrimaryVertices)
  }
}

val events = EventIterator()
