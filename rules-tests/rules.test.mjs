// Pruebas de las reglas de Firestore contra el emulador: `npm test` (dentro de rules-tests/).
import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, test } from "node:test";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import {
  arrayUnion, deleteDoc, doc, getDoc, serverTimestamp, setDoc, Timestamp, updateDoc, writeBatch, addDoc, collection,
} from "firebase/firestore";

let env;
const CID = "torre1";
const CODE = "ABC234";

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-chat-simpapp",
    firestore: { rules: readFileSync("../firestore.rules", "utf8") },
  });
});
after(async () => env.cleanup());
beforeEach(async () => env.clearFirestore());

const db = (uid) => env.authenticatedContext(uid).firestore();

/** Crea la comunidad como lo hace la app: un solo lote atómico. */
function createCommunityBatch(firestore, uid, overrides = {}) {
  const b = writeBatch(firestore);
  b.set(doc(firestore, `communities/${CID}`), {
    name: "Torres del Parque", address: "", unitsCount: 40, inviteCode: CODE, createdBy: uid,
    admins: [uid], plan: "free", createdAt: serverTimestamp(), ...overrides,
  });
  b.set(doc(firestore, `invites/${CODE}`), { communityId: CID });
  b.set(doc(firestore, `communities/${CID}/members/${uid}`), { uid, unit: "101", role: "admin", code: CODE, joinedAt: serverTimestamp() });
  b.set(doc(firestore, `chats/${CID}`), { type: "community", name: "Torres del Parque", members: [uid], createdBy: uid, updatedAt: serverTimestamp() });
  b.set(doc(firestore, `users/${uid}`), { communities: arrayUnion(CID) }, { merge: true });
  return b;
}

function joinBatch(firestore, uid, unit, code = CODE) {
  const b = writeBatch(firestore);
  b.set(doc(firestore, `communities/${CID}/members/${uid}`), { uid, unit, role: "resident", code, joinedAt: serverTimestamp() });
  b.update(doc(firestore, `chats/${CID}`), { members: arrayUnion(uid) });
  b.set(doc(firestore, `users/${uid}`), { communities: arrayUnion(CID) }, { merge: true });
  return b;
}

async function setup() {
  await createCommunityBatch(db("admin"), "admin").commit();
  await joinBatch(db("ana"), "ana", "502").commit();
}

describe("comunidades", () => {
  test("crear una comunidad deja al creador como administración", async () => {
    await assertSucceeds(createCommunityBatch(db("admin"), "admin").commit());
  });

  test("no se puede crear una comunidad ya en plan Pro", async () => {
    await assertFails(createCommunityBatch(db("admin"), "admin", { plan: "pro" }).commit());
  });

  test("unirse con el código correcto funciona; con uno incorrecto no", async () => {
    await createCommunityBatch(db("admin"), "admin").commit();
    await assertFails(joinBatch(db("eve"), "eve", "999", "ZZZ999").commit());
    await assertSucceeds(joinBatch(db("ana"), "ana", "502").commit());
  });

  test("alguien de fuera no puede leer la comunidad ni sus avisos", async () => {
    await setup();
    await assertFails(getDoc(doc(db("eve"), `communities/${CID}`)));
    await assertFails(getDoc(doc(db("eve"), `communities/${CID}/announcements/x`)));
    await assertSucceeds(getDoc(doc(db("ana"), `communities/${CID}`)));
  });

  test("nadie puede activarse el plan Pro desde la app", async () => {
    await setup();
    await assertFails(updateDoc(doc(db("admin"), `communities/${CID}`), { plan: "pro" }));
    await assertFails(updateDoc(doc(db("ana"), `communities/${CID}`), { name: "Hack" }));
    await assertSucceeds(updateDoc(doc(db("admin"), `communities/${CID}`), { name: "Torres 2" }));
  });

  test("un vecino no puede entrar al chat del edificio sin membresía", async () => {
    await setup();
    await assertFails(updateDoc(doc(db("eve"), `chats/${CID}`), { members: arrayUnion("eve") }));
  });
});

describe("avisos y emergencias", () => {
  const notice = (uid, unit, type = "notice") => ({
    title: "Corte de agua", body: "Mañana 8am", type, pinned: false, authorId: uid, authorUnit: unit, createdAt: serverTimestamp(),
  });

  test("solo administración publica avisos oficiales", async () => {
    await setup();
    await assertFails(addDoc(collection(db("ana"), `communities/${CID}/announcements`), notice("ana", "502")));
    await assertSucceeds(addDoc(collection(db("admin"), `communities/${CID}/announcements`), notice("admin", "101")));
  });

  test("cualquier vecino puede lanzar una emergencia, pero solo con su unidad", async () => {
    await setup();
    await assertSucceeds(addDoc(collection(db("ana"), `communities/${CID}/announcements`), notice("ana", "502", "emergency")));
    await assertFails(addDoc(collection(db("ana"), `communities/${CID}/announcements`), notice("ana", "101", "emergency")));
  });
});

describe("votaciones", () => {
  const PID = "p1";
  async function withPoll(closed = false, closesInMs = 86_400_000) {
    await setup();
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), `communities/${CID}/polls/${PID}`), {
        question: "¿Pintamos la fachada?", options: ["Sí", "No"], quorumPercent: 50, createdBy: "admin",
        closed, closesAt: Timestamp.fromMillis(Date.now() + closesInMs),
      });
    });
  }
  const vote = (uid, unit, option = 0) =>
    setDoc(doc(db(uid), `communities/${CID}/polls/${PID}/votes/${unit}`), { option, uid, votedAt: serverTimestamp() });

  test("los vecinos no pueden crear votaciones", async () => {
    await setup();
    await assertFails(addDoc(collection(db("ana"), `communities/${CID}/polls`), {
      question: "x", options: ["a", "b"], quorumPercent: 50, createdBy: "ana", closed: false,
      closesAt: Timestamp.fromMillis(Date.now() + 1e7),
    }));
  });

  test("un voto por unidad, sin cambiarlo y solo por la propia unidad", async () => {
    await withPoll();
    await assertFails(vote("ana", "101"));
    await assertSucceeds(vote("ana", "502", 1));
    await assertFails(vote("ana", "502", 0));
  });

  test("no se puede votar una opción inexistente ni una votación cerrada", async () => {
    await withPoll();
    await assertFails(vote("ana", "502", 5));
    await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), `communities/${CID}/polls/${PID}`), { closed: true }));
    await assertFails(vote("ana", "502", 0));
  });

  test("no se puede votar después de la fecha de cierre", async () => {
    await withPoll(false, -1000);
    await assertFails(vote("ana", "502", 0));
  });
});

describe("reportes", () => {
  const RID = "r1";
  async function withReport() {
    await setup();
    await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), `communities/${CID}/reports/${RID}`), {
      title: "Ascensor", status: "open", createdBy: "admin", unit: "101", affectedUnits: ["101"], history: [],
    }));
  }

  test("un vecino suma solo su unidad y no cambia el estado", async () => {
    await withReport();
    await assertSucceeds(updateDoc(doc(db("ana"), `communities/${CID}/reports/${RID}`), { affectedUnits: arrayUnion("502") }));
    await assertFails(updateDoc(doc(db("ana"), `communities/${CID}/reports/${RID}`), { affectedUnits: arrayUnion("303") }));
    await assertFails(updateDoc(doc(db("ana"), `communities/${CID}/reports/${RID}`), { status: "resolved" }));
    await assertSucceeds(updateDoc(doc(db("admin"), `communities/${CID}/reports/${RID}`), { status: "resolved" }));
  });
});

describe("alertas del barrio", () => {
  const alert = (uid, overrides = {}) => ({
    type: "theft", description: "", lat: 4.6, lng: -74.08, geohash: "d2g6f", communityId: CID,
    communityName: "Torres del Parque", createdBy: uid, createdAt: serverTimestamp(),
    expiresAt: Timestamp.fromMillis(Date.now() + 12 * 3600e3), confirmedBy: [], goneBy: [], ...overrides,
  });

  test("solo vecinos de una comunidad pueden publicar y ver alertas", async () => {
    await setup();
    await assertFails(addDoc(collection(db("eve"), "alerts"), alert("eve")));
    await assertSucceeds(addDoc(collection(db("ana"), "alerts"), alert("ana")));
    await assertFails(getDoc(doc(db("eve"), "alerts/x")));
  });

  test("no se pueden inventar tipos, confirmaciones ni alertas eternas", async () => {
    await setup();
    await assertFails(addDoc(collection(db("ana"), "alerts"), alert("ana", { type: "sospechoso" })));
    await assertFails(addDoc(collection(db("ana"), "alerts"), alert("ana", { confirmedBy: ["x", "y"] })));
    await assertFails(addDoc(collection(db("ana"), "alerts"), alert("ana", { expiresAt: Timestamp.fromMillis(Date.now() + 90 * 86400e3) })));
  });

  test("cada vecino solo agrega o quita su propia confirmación", async () => {
    await setup();
    await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), "alerts/a1"), { ...alert("admin"), createdAt: Timestamp.now() }));
    await assertSucceeds(updateDoc(doc(db("ana"), "alerts/a1"), { confirmedBy: arrayUnion("ana") }));
    await assertFails(updateDoc(doc(db("ana"), "alerts/a1"), { confirmedBy: arrayUnion("otro") }));
    await assertFails(updateDoc(doc(db("ana"), "alerts/a1"), { type: "noise" }));
    await assertFails(deleteDoc(doc(db("ana"), "alerts/a1")));
  });
});

describe("funciones Pro", () => {
  test("el resumen IA solo se guarda en comunidades Pro", async () => {
    await setup();
    const write = () => setDoc(doc(db("ana"), `communities/${CID}/summaries/2026-W45`), { overview: "x" });
    await assertFails(write());
    await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), `communities/${CID}`), {
      plan: "pro", planExpiresAt: Timestamp.fromMillis(Date.now() + 1e9),
    }));
    await assertSucceeds(write());
  });

  test("si el edificio crece más allá del rango pagado, deja de ser Pro", async () => {
    await setup();
    await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), `communities/${CID}`), {
      plan: "pro", planExpiresAt: Timestamp.fromMillis(Date.now() + 1e9), planMaxUnits: 30,
    }));
    // unitsCount es 40 y el plan cubre 30.
    await assertFails(setDoc(doc(db("ana"), `communities/${CID}/summaries/2026-W46`), { overview: "x" }));
    await assertFails(updateDoc(doc(db("admin"), `communities/${CID}`), { planMaxUnits: 5000 }));
  });

  test("un Pro vencido deja de contar como Pro", async () => {
    await setup();
    await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), `communities/${CID}`), {
      plan: "pro", planExpiresAt: Timestamp.fromMillis(Date.now() - 1000),
    }));
    await assertFails(setDoc(doc(db("ana"), `communities/${CID}/summaries/2026-W45`), { overview: "x" }));
  });
});
