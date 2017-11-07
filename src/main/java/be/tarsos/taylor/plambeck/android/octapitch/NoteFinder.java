package be.tarsos.taylor.plambeck.android.octapitch;

/*
           ***  Created By:  ***
		      Taylor Plambeck
		   OctaPitch - Guitar Tuner
	   Built from September 2016 - June 2017
    California Polytechnic University of Pomona
  College of Computer Engineering Senior Project
	(Pitch Recognition Credit to TarsosDSP!)

 */

import static java.lang.Math.abs;

public class NoteFinder //Class with Find function that receives the pitchInHz and outputs reference note, distance and ABS distance
{
    static double ABSdistToPitch = 0;     //Saves the distance of actual pitch to reference pitch. (Absolute value)
    static double distanceToPitch = 0;    //Saves the distance of actual pitch to reference pitch. Can be (+/-)
    static double[] noteArray;          //holds every note possible on an Electric Guitar
    static public boolean A_PERFECT_CIRCLE = false;
    static public boolean LEFT_TRIANGLE1 = false; //inner triangles are 1, numbers increase as they go further from circle
    static public boolean LEFT_TRIANGLE2 = false;
    static public boolean LEFT_TRIANGLE3 = false;
    static public boolean LEFT_TRIANGLE4 = false;
    static public boolean RT_TRIANGLE1 = false;
    static public boolean RT_TRIANGLE2 = false;
    static public boolean RT_TRIANGLE3 = false;
    static public boolean RT_TRIANGLE4 = false;


    public static String Find(float pitch)  //function that finds the REFERENCE NOTE and DISTANCE from it for the ACTUAL PITCH
    {
        String Note = "";
        if (pitch == (-1)) //if we have no audio input then display this
        {
            Note = "--";
            return Note;
        }

//this array holds ALL notes from A2-E5. -----------------------------
        noteArray = new double[61];
        noteArray[0 ] = 41.20344;  //E1         other 8th string tuning
        noteArray[1 ] = 43.65353;  //F1
        noteArray[2 ] = 46.24930;  //F#1        normal 8th string tuning
        noteArray[3 ] = 48.99943;  //G1
        noteArray[4 ] = 51.91309;  //G#1
        noteArray[5 ] = 55.00000;  //A1   Drop tuning for a 7 string guitar
        noteArray[6 ] = 58.27047;  //A#1
        noteArray[7 ] = 61.73541;  //B1      7 STRING LOW B OPEN
        noteArray[8 ] = 65.40639;  //C2
        noteArray[9 ] = 69.29566;  //C#2
        noteArray[10] = 73.41619;  //D2
        noteArray[11] = 77.78175;  //D#2
        noteArray[12] = 82.40689;  //E2        E STRING OPEN
        noteArray[13] = 87.30706;  //F2
        noteArray[14] = 92.49861;  //F#2
        noteArray[15] = 97.99886;  //G2
        noteArray[16] = 103.8262;  //G#2
        noteArray[17] = 110.0000;  //A2        A STRING OPEN
        noteArray[18] = 116.5409;  //A#2
        noteArray[19] = 123.4708;  //B2
        noteArray[20] = 130.8128;  //C3
        noteArray[21] = 138.5913;  //C#3
        noteArray[22] = 146.8324;  //D3      D STRING OPEN
        noteArray[23] = 155.5635;  //D#3
        noteArray[24] = 164.8138;  //E3
        noteArray[25] = 174.6141;  //F3
        noteArray[26] = 184.9972;  //F#2
        noteArray[27] = 195.9977;  //G3       G STRING OPEN
        noteArray[28] = 207.6523;  //G#3
        noteArray[29] = 220.0000;  //A3
        noteArray[30] = 233.0819;  //A#3
        noteArray[31] = 246.9417;  //B3       B STRING OPEN
        noteArray[32] = 261.6256;  //C4
        noteArray[33] = 277.1826;  //C#4
        noteArray[34] = 293.6648;  //D4
        noteArray[35] = 311.1270;  //D#4
        noteArray[36] = 329.6276;  //E4       HIGH E STRING OPEN
        noteArray[37] = 349.2282;  //F4
        noteArray[38] = 369.9944;  //F#4
        noteArray[39] = 391.9954;  //G4
        noteArray[40] = 415.3047;  //G#4
        noteArray[41] = 440.0000;  //A4
        noteArray[42] = 466.1638;  //A#4
        noteArray[43] = 493.8833;  //B4
        noteArray[44] = 523.2511;  //C5
        noteArray[45] = 554.3653;  //C#5
        noteArray[46] = 587.3295;  //D5
        noteArray[47] = 622.2540;  //D#5
        noteArray[48] = 659.2551;  //E5       HIGH E 12TH FRET
        noteArray[49] = 698.4565;  //F5
        noteArray[50] = 739.9888;  //F#5
        noteArray[51] = 783.9909;  //G5
        noteArray[52] = 830.6094;  //G#5
        noteArray[53] = 880.0000;  //A5
        noteArray[54] = 932.3275;  //A#5
        noteArray[55] = 987.7666;  //B5
        noteArray[56] = 1046.502;  //C6
        noteArray[57] = 1108.731;  //C#6
        noteArray[58] = 1174.659;  //D6
        noteArray[59] = 1244.508;  //D#6
        noteArray[60] = 1318.510;  //E6       THIS GOES UP TO THE HIGH E STRING, 24th FRET
//------------------------------------------------------------------------------
        //This is a Search function to find the closest (reference) note to the ACTUAL pitch
        String closestPitchName;
        double tempDistance, checkDistance; //tempDistance is the current lowest distance, check is the iterated checker
        tempDistance = abs(noteArray[0] - pitch);    //assume closest is A2 (first in array)
        int index = 0;
        for (int i = 1; i < noteArray.length; i++)    //for every note in the array search
        {
            checkDistance = abs(noteArray[i] - pitch);//find distance of next note
            if (checkDistance < tempDistance)    //if it is closer, then replace the index.
            {
                index = i;
                tempDistance = checkDistance;  //Replace current lowest
            }
        }
        ABSdistToPitch = tempDistance;      //Lowest distance from search function. THIS IS THE ABSOLUTE VALUE
        distanceToPitch = noteArray[index] - pitch;      //THIS ACTUAL DISTANCE. (CAN BE NEGATIVE)
//---------------------------------------------------------------------------------

        AnalyzeDistance(ABSdistToPitch,distanceToPitch,index); //pass ABSOLUTE DISTANCE, DISTANCETOPITCH(+/-), and INDEX to our Analyzer

//CASE STATEMENT FINDS THE NAME OF THE REFERENCE NOTE, BASED ON THE INDEX. REFERENCE NOTE IS RETURNED TO Main ACTIVITY BELOW
        switch (index) {
            case 0:
                closestPitchName = "E1";
                break;
            case 1:
                closestPitchName = "F1";
                break;
            case 2:
                closestPitchName = "F#1";
                break;
            case 3:
                closestPitchName = "G1";
                break;
            case 4:
                closestPitchName = "G#1";
                break;
            case 5:
                closestPitchName = "A1";
                break;
            case 6:
                closestPitchName = "A#1";
                break;
            case 7:
                closestPitchName = "B1";
                break;
            case 8:
                closestPitchName = "C2";
                break;
            case 9:
                closestPitchName = "C#2";
                break;
            case 10:
                closestPitchName = "D2";
                break;
            case 11:
                closestPitchName = "D#2";
                break;
            case 12:
                closestPitchName = "E2";
                break;
            case 13:
                closestPitchName = "F2";
                break;
            case 14:
                closestPitchName = "F#2";
                break;
            case 15:
                closestPitchName = "G2";
                break;
            case 16:
                closestPitchName = "G#2";
                break;
            case 17:
                closestPitchName = "A2";
                break;
            case 18:
                closestPitchName = "A#2";
                break;
            case 19:
                closestPitchName = "B2";
                break;
            case 20:
                closestPitchName = "C3";
                break;
            case 21:
                closestPitchName = "C#3";
                break;
            case 22:
                closestPitchName = "D3";
                break;
            case 23:
                closestPitchName = "D#3";
                break;
            case 24:
                closestPitchName = "E3";
                break;
            case 25:
                closestPitchName = "F3";
                break;
            case 26:
                closestPitchName = "F#3";
                break;
            case 27:
                closestPitchName = "G3";
                break;
            case 28:
                closestPitchName = "G#3";
                break;
            case 29:
                closestPitchName = "A3";
                break;
            case 30:
                closestPitchName = "A#3";
                break;
            case 31:
                closestPitchName = "B3";
                break;
            case 32:
                closestPitchName = "C4";
                break;
            case 33:
                closestPitchName = "C#4";
                break;
            case 34:
                closestPitchName = "D4";
                break;
            case 35:
                closestPitchName = "D#4";
                break;
            case 36:
                closestPitchName = "E4";
                break;
            case 37:
                closestPitchName = "F4";
                break;
            case 38:
                closestPitchName = "F#4";
                break;
            case 39:
                closestPitchName = "G4";
                break;
            case 40:
                closestPitchName = "G#4";
                break;
            case 41:
                closestPitchName = "A4";
                break;
            case 42:
                closestPitchName = "A#4";
                break;
            case 43:
                closestPitchName = "B4";
                break;
            case 44:
                closestPitchName = "C5";
                break;
            case 45:
                closestPitchName = "C#5";
                break;
            case 46:
                closestPitchName = "D5";
                break;
            case 47:
                closestPitchName = "D#5";
                break;
            case 48:
                closestPitchName = "E5";
                break;
            case 49:
                closestPitchName = "F5";
                break;
            case 50:
                closestPitchName = "F#5";
                break;
            case 51:
                closestPitchName = "G5";
                break;
            case 52:
                closestPitchName = "G#5";
                break;
            case 53:
                closestPitchName = "A5";
                break;
            case 54:
                closestPitchName = "A#5";
                break;
            case 55:
                closestPitchName = "B5";
                break;
            case 56:
                closestPitchName = "C6";
                break;
            case 57:
                closestPitchName = "C#6";
                break;
            case 58:
                closestPitchName = "D6";
                break;
            case 59:
                closestPitchName = "D#6";
                break;
            case 60:
                closestPitchName = "E6";
                break;
            default:
                closestPitchName = "--";
                break;
        }
        Note = closestPitchName;
        return Note;
    }

    public static void AnalyzeDistance(double ABSdistance, double actualDistance, int NoteIndex)
    {
        double ReferenceNotesDistance = 0.0;
        //ABSdistance is used to decide if we turn on an OUTER triangle or an INNER triangle
        //ACTUALdistance is used to decide if we illuminate a triangle on the LEFT or RIGHT, because ACTUALDISTANCE can be (+/-)
        if (actualDistance > 0)    //if ActualDistance is positive, we will light up one of the LEFT triangles.
        {
            if (NoteIndex == 0)
                ReferenceNotesDistance = 2.4; //this prevents out of bounds, and sets the distance of the lowest note to the second lowest
            if (NoteIndex != 0)
            {
                ReferenceNotesDistance = abs(noteArray[NoteIndex] - noteArray[NoteIndex - 1]);
                //ReferenceNotesDistance=distance between the 2 closest notes to ACTUAL. Used as the max distance between two closest notes.
                //  (This is decided dynamically because higher notes are farther apart)
                //Based on my algorithm, the reference note we chose must be closer to ACTUAL than the note on the other side of it.
                //Thus, <50% is assumed.  The if statements below check if we are 49% or 2.5%. This is left side, so it would be % too low.
            }
/* Left Begin */
            if (ReferenceNotesDistance * (0.025) > ABSdistance) //if the distance is within 2.5% of the note then turn on the CIRCLE
            {
                A_PERFECT_CIRCLE = true;//flag
            }
            else
            if (ReferenceNotesDistance * (0.15) > ABSdistance) //if the distance is within 15% of the note then turn on LEFT TRIANGLE1
            {
                LEFT_TRIANGLE1 = true;//flag
            }
            else
            if (ReferenceNotesDistance * (0.20) > ABSdistance) //if the distance is within 20% of the note then turn on LEFT TRIANGLE2
            {
                LEFT_TRIANGLE2 = true;//flag
            }
            else
            if (ReferenceNotesDistance * (0.35) > ABSdistance) //if the distance is within 35% of the note then turn on LEFT TRIANGLE3
            {
                LEFT_TRIANGLE3 = true;//flag
            }
            else
                LEFT_TRIANGLE4 = true;  // IF WE ARE 36%-49% too LOW, turn on LEFT TRIANGLE4
        }
        else    // LEFT ENDS, RIGHT SIDE BEGINS
        {
            if (NoteIndex != 60)
            {
                ReferenceNotesDistance = abs(noteArray[NoteIndex] - noteArray[NoteIndex + 1]);     //distance between the two closest notes.
                //we assume that if we chose the reference note, we must be closer to it than to the one on the otherside of the ACTUAL note.
                //Thus, 50% is assumed.  The only thing to check here is if we're either 49% or super close
            }
            if (NoteIndex == 60)
                ReferenceNotesDistance = 74.0;    //this prevents out of bounds, and sets the distance of the highest note to the second highest
/* Right Begin */
            if (ReferenceNotesDistance * (0.025) > ABSdistance) //if the distance is within 2.5% of the note then turn PERFECT CIRCLE
            {
                A_PERFECT_CIRCLE = true;
            }
            else
            if (ReferenceNotesDistance * (0.15) > ABSdistance) //if the distance is within 15% of the note then turn on RIGHT TRIANGLE1
            {
                RT_TRIANGLE1 = true;
            }
            else
            if (ReferenceNotesDistance * (0.20) > ABSdistance) //if the distance is within 20% of the note then turn on RIGHT TRIANGLE2
            {
                RT_TRIANGLE2 = true;
            }
            else
            if (ReferenceNotesDistance * (0.35) > ABSdistance) //if the distance is within 35% of the note then turn on RIGHT TRIANGLE3
            {
                RT_TRIANGLE3 = true;
            }
            else
                RT_TRIANGLE4= true;  // If we are 36%-49% too HIGH, turn on RIGHT TRIANGLE4
        }   //end of Else
    }   //end of Analyze function

}  //end class