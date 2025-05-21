---------------- MODULE missionaries ----------------
(* Le problème des cannibales et des missionaires *)

EXTENDS Naturals, FiniteSets

CONSTANTS nbC, nbM, capaBarque

VARIABLES
    nbCannibalesG,
    nbCannibalesD,
    nbMissionnairesG,
    nbMissionnairesD,
    barque

ASSUME nbC \in Nat /\ nbM \in Nat /\ capaBarque \in Nat
    
Rives == { "G", "D" }

TypeOK ==
  [] (/\ nbCannibalesG \in 0..nbC
      /\ nbCannibalesD \in 0..nbC
      /\ nbMissionnairesG \in 0..nbM
      /\ nbMissionnairesD \in 0..nbM
      /\ barque \in Rives)

pasMiam ==
    /\ (nbMissionnairesG > 0 => nbCannibalesG <= nbMissionnairesG)
    /\ (nbMissionnairesD > 0 => nbCannibalesD <= nbMissionnairesD)

ToujoursOk == []pasMiam

Solution ==
  [] \neg(nbCannibalesG = 0 /\ nbCannibalesD = nbC /\ nbMissionnairesG = 0 /\ nbMissionnairesD = nbM)

----------------------------------------------------------------

inv(r) == IF r = "G" THEN "D" ELSE "G"

Init ==
  /\ barque = "G"
  /\ nbCannibalesD = 0
  /\ nbCannibalesG = nbC
  /\ nbMissionnairesD = 0
  /\ nbMissionnairesG = nbM
  /\ pasMiam
  
bougeGD(nbMissionnaire, nbCannibale) ==
    /\ nbMissionnaire + nbCannibale <= capaBarque
    /\ nbMissionnaire <= nbMissionnairesG
    /\ nbCannibale <= nbCannibalesG
    /\ nbCannibalesG' = nbCannibalesG - nbCannibale
    /\ nbCannibalesD' = nbCannibalesD + nbCannibale
    /\ nbMissionnairesG' = nbMissionnairesG - nbMissionnaire
    /\ nbMissionnairesD' = nbMissionnairesD + nbMissionnaire
    /\ pasMiam'
    /\ barque' = "D"

bougeDG(nbMissionnaire, nbCannibale) ==
    /\ nbMissionnaire + nbCannibale <= capaBarque
    /\ nbMissionnaire <= nbMissionnairesD
    /\ nbCannibale <= nbCannibalesD
    /\ nbCannibalesD' = nbCannibalesD - nbCannibale
    /\ nbCannibalesG' = nbCannibalesG + nbCannibale
    /\ nbMissionnairesD' = nbMissionnairesD - nbMissionnaire
    /\ nbMissionnairesG' = nbMissionnairesG + nbMissionnaire
    /\ pasMiam'
    /\ barque' = "G"

Next == (\E nm \in 0..nbM, nc \in 0..nbC : bougeGD(nm,nc)) \/ (\E nm \in 0..nbM, nc \in 0..nbC : bougeDG(nm,nc))

Spec == Init /\ [] [ Next ]_<<nbCannibalesG, nbCannibalesD, nbMissionnairesG, nbMissionnairesD, barque>>

================================================================
